package ccio.imman.http;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request; 
import org.glassfish.grizzly.http.server.Response;

import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty; 

public class RootHttpHandler extends HttpHandler {
	
	private static final DynamicStringProperty ROOT_REDIRECT = DynamicPropertyFactory.getInstance().getStringProperty("root.redirect.url", "http://cloudcluster.io");

	@Override
	public void service(Request request, Response response) throws Exception {
		response.sendRedirect(ROOT_REDIRECT.get());
		response.finish();
	}
}