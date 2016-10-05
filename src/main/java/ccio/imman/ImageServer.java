package ccio.imman;

import java.io.IOException;
import java.util.ArrayList;

import org.glassfish.grizzly.http.server.ErrorPageGenerator;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringListProperty;
import com.netflix.config.DynamicStringProperty;

import ccio.imman.http.ImageHttpHandler;
import ccio.imman.http.RootHttpHandler;
import ccio.imman.http.StatisticsHttpHandler;
import ccio.imman.manipulation.ThumbnailatorManipulation;
import ccio.imman.origin.AwsS3Loader;

public class ImageServer {

	static {
		System.setProperty("archaius.configurationSource.additionalUrls", "file:/opt/ccio-imman.properties");
		SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)
		SLF4JBridgeHandler.install();
	}
	
	public static final DynamicStringProperty SECRET = DynamicPropertyFactory.getInstance().getStringProperty("secret", "KQJHNdkjsnd98JHbsdkjk");
	public static final DynamicStringProperty SERVER_IP_PRIVATE = DynamicPropertyFactory.getInstance().getStringProperty("http.ip.private", "127.0.0.1");
	public static final DynamicStringProperty SERVER_IP_PUBLIC = DynamicPropertyFactory.getInstance().getStringProperty("http.ip.public", "127.0.0.1");
	public static final DynamicIntProperty SERVER_PORT_PRIVATE = DynamicPropertyFactory.getInstance().getIntProperty("http.port.private", 8002);
	public static final DynamicIntProperty SERVER_PORT_PUBLIC = DynamicPropertyFactory.getInstance().getIntProperty("http.port.public", 8001);
	public static final DynamicStringListProperty NODES = new DynamicStringListProperty("nodes", new ArrayList<>());
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ImageServer.class);

	public static void main(String[] args) {

		LOGGER.info("Starting CCIO ImMan HTTP Server");
		
		ImageHttpHandler httpHandler = new ImageHttpHandler(new ThumbnailatorManipulation(), new AwsS3Loader());
		StatisticsHttpHandler statisticsHttpHandler = new StatisticsHttpHandler();
		
		final HttpServer serverPublic = HttpServer.createSimpleServer("/", SERVER_IP_PUBLIC.get(), SERVER_PORT_PUBLIC.get());
		final ServerConfiguration config = serverPublic.getServerConfiguration();
		config.addHttpHandler(new RootHttpHandler(), "/");
		config.addHttpHandler(httpHandler, "/*");
		config.addHttpHandler(statisticsHttpHandler, "/stat/*");
		config.setHttpServerName("ccio-imman");
		config.setDefaultErrorPageGenerator(new ErrorPageGenerator() {
			@Override
			public String generate(Request request, int status, String reasonPhrase, String description, Throwable e) {
				if(status > 500){
					LOGGER.error("Status {}, Reason {}, Description {}, Exception {}", status, reasonPhrase, description, e);
				}
				return null;
			}
		});
		final HttpServer serverPrivate = HttpServer.createSimpleServer("/", SERVER_IP_PRIVATE.get(), SERVER_PORT_PRIVATE.get());
		final ServerConfiguration configPrivate = serverPrivate.getServerConfiguration();
		configPrivate.addHttpHandler(statisticsHttpHandler, "/stat/*");
		configPrivate.addHttpHandler(httpHandler, "/*");
		configPrivate.setDefaultErrorPageGenerator(new ErrorPageGenerator() {
			@Override
			public String generate(Request request, int status, String reasonPhrase, String description, Throwable e) {
				if(status > 500){
					LOGGER.error("Status {}, Reason {}, Description {}, Exception {}", status, reasonPhrase, description, e);
				}
				return null;
			}
		});
		try {
			serverPublic.start();
			serverPrivate.start();
			LOGGER.info("CCIO ImMan HTTP Server is started");
			Thread.currentThread().join();
		} catch (IOException | InterruptedException e1) {
			LOGGER.error("Failed to start CCIO ImMan HTTP Server: {}", e1);
		} 
	}		
}
