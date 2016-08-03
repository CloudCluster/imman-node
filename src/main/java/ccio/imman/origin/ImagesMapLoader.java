package ccio.imman.origin;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.Member;

import ccio.imman.FileInfo;
import ccio.imman.ImageServer;

public class ImagesMapLoader extends LocalFileLoader implements HazelcastInstanceAware {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ImagesMapLoader.class);
	
	private final AwsS3Loader s3Loader;
	private IExecutorService executorService;
	
	public ImagesMapLoader() {
		s3Loader = new AwsS3Loader();
	}

	@Override
	public byte[] load(FileInfo fileInfo) {
		LOGGER.debug("Loading: {}", fileInfo);
		if(fileInfo.isOriginalFile()){
			byte[] res = super.load(fileInfo);
			if(res == null){
				//the file is not in the cluster, lets get it from origin
				//but maybe it is on some other node somehow, let's re-balance if so
				Map<Member, Future<byte[]>> futures = executorService.submitToAllMembers(new FileRebalanceFunction(fileInfo));
				for(Future<byte[]> future : futures.values()){
					try {
						res = future.get();
						if(res != null){
							LocalFileLoader.storeFile(fileInfo.canonicalPath(), res);
							break;
						}
					} catch (InterruptedException | ExecutionException e) {
						LOGGER.debug(e.getMessage(), e);
					}
				}
				
				if(res == null){
					//the file cannot be found anywhere, lets load it from origin
					res = s3Loader.load(fileInfo);
					storeFile(fileInfo.getPath(), res);
				}
			}
			return res;
		}else{
			return null;
		}
	}

	@Override
	public void setHazelcastInstance(HazelcastInstance hzInstance) {
		this.executorService = hzInstance.getExecutorService(ImageServer.NAME_REMOTE_FILE_EXEC);		
	}

}
