package ccio.imman.origin;

import java.io.File;
import java.util.Iterator;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicPropertyFactory;

public class RemoveOldFiles implements Runnable{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoveOldFiles.class);
	
	private static final DynamicLongProperty FILES_MIN_FREE_SPACE = DynamicPropertyFactory.getInstance().getLongProperty("files.space.reserved", 1000000000L);

	@Override
	public void run() {
		try{
			int filesRemoved = 0;

			for(String location : LocalFileLoader.FILES_LOCATIONS.get()) {
				if(LOGGER.isDebugEnabled()){
					long usableSpace = new File(location).getUsableSpace();
					LOGGER.debug("Removing files if there is no space anymore. Usable space: {}, Min free space: {}, Still available {}", usableSpace, FILES_MIN_FREE_SPACE.get(), usableSpace - FILES_MIN_FREE_SPACE.get());
					LOGGER.trace("LAST_ACCESS Before: {}", LocalFileLoader.LAST_ACCESS);
				}

				while (FILES_MIN_FREE_SPACE.get() > new File(location).getUsableSpace() && LocalFileLoader.LAST_ACCESS.size() > 0) {
					// we are removing LRU files
					String toDeleteKey = null;
					Long toDeleteTime = Long.MAX_VALUE;
			
					for (Iterator<Entry<String, Long>> i = LocalFileLoader.LAST_ACCESS.entrySet().iterator(); i.hasNext();) {
						Entry<String, Long> entry = i.next();
						long l = entry.getValue();
						if (toDeleteTime > l) {
							toDeleteTime = l;
							toDeleteKey = entry.getKey();
						}
					}
				
					LOGGER.debug("KEY to remove: {}", toDeleteKey);
					
					if(toDeleteKey != null){
						filesRemoved++;
						LocalFileLoader.deleteFileByKey(toDeleteKey);
						LocalFileLoader.LAST_ACCESS.remove(toDeleteKey);
					}
				}
			}
			LOGGER.debug("Files removed {}", filesRemoved);
		} catch(Throwable t) {
			LOGGER.error("Free space check failed", t);
		}
		LOGGER.trace("LAST_ACCESS After: {}", LocalFileLoader.LAST_ACCESS);		
	}

}
