package ccio.imman.http;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.glassfish.grizzly.http.io.NIOOutputStream;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.threadpool.GrizzlyExecutorService;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;

import ccio.imman.FileInfo;
import ccio.imman.ImageServer;
import ccio.imman.Manipulation;

public class ImageHttpHandler extends HttpHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ImageHttpHandler.class);
	
	private static final DynamicIntProperty POOL_CORE = DynamicPropertyFactory.getInstance().getIntProperty("http.executor.pool.core", 100);
	private static final DynamicIntProperty POOL_MAX = DynamicPropertyFactory.getInstance().getIntProperty("http.executor.pool.max", 150);
	private static final DynamicIntProperty EXPIRED_IN_SECONDS = DynamicPropertyFactory.getInstance().getIntProperty("http.image.expired.in.seconds", 31104000);
	
	final ExecutorService executorService = GrizzlyExecutorService.createInstance(
	            ThreadPoolConfig.defaultConfig()
	            .copy()
	            .setCorePoolSize(POOL_CORE.get())
	            .setMaxPoolSize(POOL_MAX.get()));
	
	private final Map<FileInfo, byte[]> imagesMap;
	private final Manipulation manipulation;

	public ImageHttpHandler(HazelcastInstance hzInstance, Manipulation manipulation) {
		this.imagesMap = hzInstance.getMap(ImageServer.NAME_MAP_IMAGES);
		this.manipulation = manipulation;
	}

	@Override
	public void service(Request request, Response response) throws Exception {
		
		response.suspend(); // Instruct Grizzly to not flush response, once we exit the service(...) method
		
		executorService.execute(new Runnable() {   // Execute long-lasting task in the custom thread
            public void run() {
            	byte[] res = null;
                try {
                	final FileInfo fileInfo = new FileInfo(request);
                	res = imagesMap.get(fileInfo);
                	
                	if(res == null && !fileInfo.isOriginalFile()){
            			FileInfo originalFileInfo = new FileInfo();
            			originalFileInfo.setPath(fileInfo.getPath());
            			res = imagesMap.get(originalFileInfo);
            			if(res != null) {
        					res = manipulation.manipulate(res, fileInfo);
        					//not to store resized file but putting it into cache
        					//because it looks like CloudFlare is caching the file before it gets evicted
        					if(res != null){
        						imagesMap.put(fileInfo, res);
//        						LocalFileLoader.storeFile(fileInfo.canonicalPath(), res);
        					}
            			}
                	}
            		if(res != null){
            			generateResponse(res, fileInfo, response);
            		} else {
		        		// we didn't find the file anywhere, returning 404
            			generateNotFoundResponse(response);
            		}
                } catch (Throwable e) {
                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                    if (response.isSuspended()) {
                		response.resume();
                	} else {
                		response.finish();                   
                	}
                    LOGGER.error(e.getMessage(), e);
                    res = null;
                }
            }
            
        	// privates
        	private void generateNotFoundResponse(Response response) {
        		response.setStatus(HttpStatus.NOT_FOUND_404);
        		if (response.isSuspended()) {
            		response.resume();
            	} else {
            		response.finish();                   
            	}
        	}
        	
        	private void generateResponse(byte[] bytes, FileInfo fileInfo, Response response)
        			throws IOException {
        		
        		response.setContentType(fileInfo.contentType());
        		response.setHeader("Access-Control-Allow-Origin", "*");
//        		response.setHeader("Last-Modified", new Date(lastModif).toString());
        		response.setHeader("Cache-Control", "max-age="+EXPIRED_IN_SECONDS.get());
        		response.setDateHeader("Expires", System.currentTimeMillis()+EXPIRED_IN_SECONDS.get()*1000);
        		response.setHeader("Pragma", (String)null);
        		response.setContentLengthLong(bytes.length);
        		response.setStatus(HttpStatus.OK_200);
        		
        		if(bytes != null){
        			NIOOutputStream out = response.getNIOOutputStream(); 
        			out.notifyCanWrite(new BytesWriteHandler(bytes, response, out){
        	
        				@Override
        				public void onError(Throwable t) {
        	            	response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);	
        	            	if (response.isSuspended()) {
        	            		response.resume();
        	            	} else {
        	            		response.finish();                   
        	            	}
        				}
        			});
//        			response.getOutputStream().flush();
        		}
        	}
        });
	}
}
