package ccio.imman.origin;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import com.ning.http.client.providers.grizzly.GrizzlyAsyncHttpProvider;

import ccio.imman.FileInfo;
import ccio.imman.ImageServer;

public class ProxyLoader {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ProxyLoader.class);
	
	private final static AsyncHttpClientConfig CONFIG = new AsyncHttpClientConfig.Builder().setMaxConnections(50).build();
	private final static AsyncHttpClient CLIENT = new AsyncHttpClient(new GrizzlyAsyncHttpProvider(CONFIG), CONFIG);
	
	public static byte[] load(String ip, FileInfo fileInfo){
		StringBuilder sb = new StringBuilder("http://")
				.append(ip)
				.append(":")
				.append(ImageServer.SERVER_PORT_PRIVATE.get())
//				.append("/")
				.append(fileInfo.canonicalPath());
		LOGGER.debug("Proxy to {}", sb);
		final Request request = new RequestBuilder("GET").setUrl(sb.toString()).build();
		try {
			Future<Response> responseFuture = CLIENT.executeRequest(request, new AsyncCompletionHandler<Response>() {
		 
				@Override
				public Response onCompleted(Response response) throws Exception {
					return response;
				}
				 
				@Override
				public void onThrowable(Throwable t) {
					LOGGER.debug(t.getMessage(), t);
				}
				 
			});
			final Response response = responseFuture.get(15, TimeUnit.SECONDS);
			return response.getResponseBodyAsBytes();
		} catch (Throwable t) {
			LOGGER.debug(t.getMessage(), t);
		}
		return null;
	}
}
