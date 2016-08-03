package ccio.imman;

import java.io.IOException;
import java.util.Properties;

import org.glassfish.grizzly.http.server.ErrorPageGenerator;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.google.common.collect.Lists;
import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.MaxSizeConfig.MaxSizePolicy;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MapLoader;
import com.hazelcast.core.MapStoreFactory;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringListProperty;
import com.netflix.config.DynamicStringProperty;

import ccio.imman.http.ImageHttpHandler;
import ccio.imman.http.RootHttpHandler;
import ccio.imman.http.StatisticsHttpHandler;
import ccio.imman.manipulation.ThumbnailatorManipulation;
import ccio.imman.origin.ImagesMapLoader;

public class ImageServer {

	static {
		System.setProperty("archaius.configurationSource.additionalUrls", "file:/opt/ccio-imman.properties");
		SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)
		SLF4JBridgeHandler.install();
	}
	
	public static final String NAME_REMOTE_FILE_EXEC = "remoteFileExec";
	public static final String NAME_MAP_IMAGES = "images";
	public static final DynamicStringProperty SECRET = DynamicPropertyFactory.getInstance().getStringProperty("secret", "KQJHNdkjsnd98JHbsdkjk");

	private static final Logger LOGGER = LoggerFactory.getLogger(ImageServer.class);
	
	private static final DynamicStringProperty SERVER_IP = DynamicPropertyFactory.getInstance().getStringProperty("http.ip", "127.0.0.1");
	private static final DynamicIntProperty SERVER_PORT = DynamicPropertyFactory.getInstance().getIntProperty("http.port", 8001);
	private static final DynamicStringProperty TRANSPORT_IP = DynamicPropertyFactory.getInstance().getStringProperty("transport.ip", "127.0.0.1");
	private static final DynamicIntProperty TRANSPORT_PORT = DynamicPropertyFactory.getInstance().getIntProperty("transport.port", 9901);
	private static final DynamicStringListProperty SEEDS = new DynamicStringListProperty("transport.seeds", "127.0.0.1:9901,127.0.0.1:9902");
	private static final DynamicStringProperty CLUSTER_NAME = DynamicPropertyFactory.getInstance().getStringProperty("name", "ccio-imman");
	private static final DynamicIntProperty MAP_FREE_HEAP_PERCENTAGE = DynamicPropertyFactory.getInstance().getIntProperty("heap.percentage", 50);

	public static void main(String[] args) {

		LOGGER.info("Starting CCIO ImMan Cluster");
		Config hzConfig = new Config();
		hzConfig.setProperty("hazelcast.logging.type", "slf4j");
		hzConfig.setProperty("hazelcast.max.no.heartbeat.seconds", "10");
		
		hzConfig.getNetworkConfig()
			.setPort(TRANSPORT_PORT.get())
			.setPortAutoIncrement(true);
		
		hzConfig.getNetworkConfig()
			.getInterfaces()
			.setEnabled(true)
			.setInterfaces(Lists.newArrayList(TRANSPORT_IP.get()));
		
		hzConfig.getNetworkConfig()
			.getJoin()
			.getMulticastConfig()
			.setEnabled(false);
		
		hzConfig.getNetworkConfig()
			.getJoin()
			.getTcpIpConfig()
			.setEnabled(true)
			.setMembers(SEEDS.get());
		
		hzConfig.getGroupConfig()
			.setName(CLUSTER_NAME.get())
			.setPassword(SECRET.get());
		
//		if(MANAGMENT_CENTER_URL.get()!=null){
//			hzConfig.getManagementCenterConfig()
//				.setEnabled(true)
//				.setUrl(MANAGMENT_CENTER_URL.get());
//		}
		
		hzConfig.getExecutorConfig(NAME_REMOTE_FILE_EXEC)
			.setPoolSize(250)
			.setQueueCapacity(250)
			.setStatisticsEnabled(false);
		
		
		// Map Config
		hzConfig.getMapConfig(NAME_MAP_IMAGES)
			.setBackupCount(0)
			.setAsyncBackupCount(0)
			.setEvictionPolicy(EvictionPolicy.LRU)
			.setEvictionPercentage(10);
		
		hzConfig.getMapConfig(NAME_MAP_IMAGES)
			.getMaxSizeConfig()
			.setMaxSizePolicy(MaxSizePolicy.FREE_HEAP_PERCENTAGE)
			.setSize(MAP_FREE_HEAP_PERCENTAGE.get());
		
		ImagesMapLoader mapLoader = new ImagesMapLoader();
		hzConfig.getMapConfig(NAME_MAP_IMAGES)
			.getMapStoreConfig()
			.setEnabled(true)
			.setFactoryImplementation(new MapStoreFactory<FileInfo, byte[]>() {
				@Override
				public MapLoader<FileInfo, byte[]> newMapStore(String mapName, Properties properties) {
					LOGGER.debug("Loader for {}", mapName);
					if(NAME_MAP_IMAGES.equals(mapName)){
						LOGGER.debug("New ImagesMapLoader is created");
						return mapLoader;
					}
					return null;
				}
			});
		
		HazelcastInstance hzInstance = Hazelcast.newHazelcastInstance(hzConfig);
		mapLoader.setHazelcastInstance(hzInstance);
		
		LOGGER.info("Starting CCIO ImMan HTTP Server");
		
		final HttpServer server = HttpServer.createSimpleServer("/", SERVER_IP.get(), SERVER_PORT.get());
		final ServerConfiguration config = server.getServerConfiguration();
		config.addHttpHandler(new RootHttpHandler(), "/");
		config.addHttpHandler(new StatisticsHttpHandler(hzInstance), "/stat/*");
		config.addHttpHandler(new ImageHttpHandler(hzInstance, new ThumbnailatorManipulation()), "/*");
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
		try {
			server.start();
			LOGGER.info("CCIO ImMan HTTP Server is started");
		} catch (IOException e1) {
			LOGGER.error("Failed to start CCIO ImMan HTTP Server: {}", e1);
		} 
	}		
}
