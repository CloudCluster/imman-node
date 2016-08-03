package ccio.imman.origin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncLastAccess implements Runnable{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SyncLastAccess.class);

	@Override
	public void run() {
		for(String location : LocalFileLoader.FILES_LOCATIONS.get()) {
			final int filePrefixIndx = location.length()+1;
			LOGGER.debug("Syncing Last Used Time on {}", location);
			try{
				int totalFilesRead = 0;
				for (Iterator<Path> iter=Files.walk(Paths.get(location)).iterator(); iter.hasNext();) {
					Path path = iter.next();
					if(path.toFile().isFile()){
						BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
						if(attrs.isRegularFile()){
							FileTime time = attrs.lastAccessTime();
							LocalFileLoader.LAST_ACCESS.put(path.toAbsolutePath().toString().substring(filePrefixIndx), time.toMillis());
							totalFilesRead++;
							Thread.sleep(2);
						}
					}
				}
				if(LOGGER.isDebugEnabled()){
					LOGGER.debug("Files read: {}, Files in the map: {}", totalFilesRead, LocalFileLoader.LAST_ACCESS.size());
				}
			}catch(Throwable e){
				LOGGER.error(e.getMessage(), e);
			}
		}		
	}

}
