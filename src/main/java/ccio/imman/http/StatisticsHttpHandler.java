package ccio.imman.http;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.threadpool.GrizzlyExecutorService;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberLeftException;

import ccio.imman.ImageServer; 

public class StatisticsHttpHandler extends HttpHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsHttpHandler.class);	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	private HazelcastInstance hzInstance;
	private final ExecutorService executorService = GrizzlyExecutorService.createInstance(
            ThreadPoolConfig.defaultConfig()
            .copy()
            .setCorePoolSize(10)
            .setMaxPoolSize(15));

	public StatisticsHttpHandler(HazelcastInstance hzInstance) {
		super();
		this.hzInstance = hzInstance;
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
				res.put("state", hzInstance.getCluster().getClusterState());
		
				Set<Map<String, Object>> members = new HashSet<>();
				IExecutorService executorService = hzInstance.getExecutorService(ImageServer.NAME_REMOTE_FILE_EXEC);
				Map<Member, Future<String>> futures = executorService.submitToAllMembers(new StatisticsFunction(hzInstance));
				LOGGER.debug("Stat retreaval submited to {} members", futures.size());
				for (Entry<Member, Future<String>> entity : futures.entrySet()) {
					try {
						LOGGER.debug("Calling Remote Member: {}", entity.getKey());
						Future<String> future = entity.getValue(); 
						String remoteStatStr = future.get();
						LOGGER.debug("Remote Stat: {}", remoteStatStr);
						HashMap<String, Object> remoteStat = new HashMap<>();
						remoteStat.put("node", getMemberInfo(entity.getKey()));
						remoteStat.put("imagesMap", MAPPER.readValue(remoteStatStr, Map.class));
						members.add(remoteStat);
					} catch (MemberLeftException e) {
						LOGGER.debug("Cannot get stat from a member, it left", e);
						HashMap<String, Object> resMember = new HashMap<>();
						resMember.put("node", getMemberInfo(e.getMember()));
						resMember.put("error", e.getMessage());
						members.add(resMember);
					} catch (InterruptedException | ExecutionException | IOException e) {
						LOGGER.debug("Cannot get stat from a member", e);
						HashMap<String, Object> resMember = new HashMap<>();
						resMember.put("error", e.getMessage());
						members.add(resMember);
					}
				}
				res.put("members", members);
				res.put("size", members.size());
				
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
            
            private Map<String, Object> getMemberInfo(Member member){
            	HashMap<String, Object> resMember = new HashMap<>();
            	resMember.put("address", member.getAddress().getHost()+":"+member.getAddress().getPort());
            	resMember.put("id", member.getUuid());
            	resMember.put("local", member.localMember());
            	return resMember;
            }
		});
	}
}