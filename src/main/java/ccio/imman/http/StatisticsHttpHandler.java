package ccio.imman.http;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.threadpool.GrizzlyExecutorService;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import ccio.imman.ImageServer;
import ccio.imman.origin.LocalFileLoader; 

public class StatisticsHttpHandler extends HttpHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsHttpHandler.class);	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	private final ExecutorService executorService = GrizzlyExecutorService.createInstance(
            ThreadPoolConfig.defaultConfig()
            .copy()
            .setCorePoolSize(10)
            .setMaxPoolSize(15));

	public StatisticsHttpHandler() {
		super();
	}

	@Override
	public void service(Request request, Response response) throws Exception {
		response.suspend(); // Instruct Grizzly to not flush response, once we exit the service(...) method
		
		executorService.execute(new Runnable() {   // Execute long-lasting task in the custom thread

			public void run() {
				String secret = request.getPathInfo();
				if(secret !=null && secret.length() > 0){
					secret = secret.substring(1);
				}
				if(!ImageServer.SECRET.get().equals(secret)){
					response.setStatus(HttpStatus.UNAUTHORIZED_401);
					response.finish();
					response.resume();
					return;			
				}
				
				HashMap<String, Object> res = new HashMap<>();
				res.put("status", "ok");
				for(String location : LocalFileLoader.FILES_LOCATIONS.get()){
					res.put(location, new File(location).getUsableSpace());
				}
				res.put("lastAccessSize", LocalFileLoader.LAST_ACCESS.size());
		
				try{
					response.setContentType("application/json");
					response.setHeader("Access-Control-Allow-Origin", "*");
					response.getOutputStream().write(MAPPER.writeValueAsBytes(res));
					response.getOutputStream().flush();
					response.setStatus(HttpStatus.OK_200);
				}catch(IOException e){
					LOGGER.debug(e.getMessage(), e);
					response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
				}
				response.finish();					
				response.resume();
			}
            
		});
	}
}