package ccio.imman.origin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicStringSetProperty;

import ccio.imman.FileInfo;

public class LocalFileLoader extends AbstractMapLoader {

	private static final Logger LOGGER = LoggerFactory.getLogger(LocalFileLoader.class);
	
	public static final DynamicStringSetProperty FILES_LOCATIONS = new DynamicStringSetProperty("files.locations", new HashSet<String>(Arrays.asList("/opt/ccio/store")));
	public static final ConcurrentHashMap<String, Long> LAST_ACCESS = new ConcurrentHashMap<>();

	private static final ScheduledExecutorService EXEC_REMOVE_FILES = Executors.newSingleThreadScheduledExecutor();
	private static final ScheduledExecutorService EXEC_SYNC_LAST_ACCESS = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
			private ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();
		
			@Override
			public Thread newThread(Runnable r) {
				Thread t = defaultThreadFactory.newThread(r);
				t.setPriority(Thread.MIN_PRIORITY);
				return t;
			}
		});
	
	static{
		FILES_LOCATIONS.addCallback(new Runnable() {
			public void run() {
				initLocations();
			}
		});
		
		initLocations();
		
		EXEC_REMOVE_FILES.scheduleWithFixedDelay(new RemoveOldFiles(), 15, 15, TimeUnit.SECONDS);
		EXEC_SYNC_LAST_ACCESS.scheduleWithFixedDelay(new SyncLastAccess(), 0, 4, TimeUnit.HOURS);		
	}
	
	@Override
	public byte[] load(FileInfo fileInfo) {
		return loadFile(fileInfo);
	}
	
	public static byte[] loadFile(FileInfo fileInfo){
		LOGGER.debug("Loading local file: {}", fileInfo);
		final String canonicalPath = fileInfo.canonicalPath();
		final String fileName = fileName(canonicalPath);
		
		for(String location : FILES_LOCATIONS.get()){
			try {
				byte[] res = Files.readAllBytes(Paths.get(location, fileName));
				if(res != null){
					LAST_ACCESS.put(fileName, System.currentTimeMillis());
					return res;
				}
			} catch (IOException e) {
				LOGGER.trace("Cannot read file {} -- {}\n{}", canonicalPath, fileName, e);
			}
		}
		return null;
	}
	
	public static void storeFile(String filePath, byte[] bytes){
		if(bytes != null){
			final String fileName = fileName(filePath);
			LOGGER.debug("Writing file {} --> {}", filePath, fileName);
			try {
				Path path = Paths.get(findFreeSpaceLocation(), fileName);
				Files.createDirectories(path.getParent());
				Files.write(path, bytes);
				LAST_ACCESS.put(fileName, System.currentTimeMillis());
			} catch (IOException e) {
				LOGGER.error("Failed to write file {} --> {}\n{}", filePath, fileName, e);
			}
		}
	}
	
	public static void deleteFile(String filePath){
		deleteFileByKey(fileName(filePath));
	}
	
	public static void deleteFileByKey(String filePathKey){
		for(String location : FILES_LOCATIONS.get()){
			try {
				removeFileAndParentsIfEmpty(Paths.get(location, filePathKey), location);
				LAST_ACCESS.remove(filePathKey);
			} catch (IOException e) {
				LOGGER.error("Failed to delete file {}", filePathKey, e);
			}
		}
	}
	
	private static void initLocations(){
		for(String location : FILES_LOCATIONS.get()){
			try {
				Files.createDirectories(Paths.get(location));
			} catch (IOException e) {
				LOGGER.error("Cannot create storage directory", e);
			}
		}
	}
	
	private static String findFreeSpaceLocation(){
		String result = null;
		long freeSpace = 0;
		for(String location : FILES_LOCATIONS.get()){
			long space = new File(location).getUsableSpace();
			if(freeSpace < space){
				freeSpace = space;
				result = location;
			}
		}
		return result;
	}
	
	private static String fileName(String filePath) {
		String fileName=DigestUtils.md5Hex(filePath);
		if(fileName.length()>2){
			fileName=fileName.substring(0, 2)+File.separator+fileName.substring(2, fileName.length());
		}
		if(fileName.length()>5){
			fileName=fileName.substring(0, 5)+File.separator+fileName.substring(5, fileName.length());
		}
		if(fileName.length()>8){
			fileName=fileName.substring(0, 8)+File.separator+fileName.substring(8, fileName.length());
		}
		return fileName;
	}
	
	private static void removeFileAndParentsIfEmpty(Path path, String location) throws IOException {
		if (path == null || path.endsWith(location)) {
			return;
		}
		if (Files.isRegularFile(path)) {
			Files.deleteIfExists(path);
		} else if (Files.isDirectory(path)) {
			if (path.toFile().list().length == 0) {
				Files.delete(path);
				//Sometimes for some reason folder still exists for a few ms.
				int i=0;
				while (Files.exists(path) && i++<20){
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						LOGGER.error(e.getMessage(), e);
					}
				}
			} else {
				return;
			}
		} else {
			return;
		}

		removeFileAndParentsIfEmpty(path.getParent(), location);
	}
}
