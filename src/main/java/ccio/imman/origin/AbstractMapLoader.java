package ccio.imman.origin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.hazelcast.core.MapLoader;

import ccio.imman.FileInfo;

public abstract class AbstractMapLoader implements MapLoader<FileInfo, byte[]> {
	
	@Override
	public Map<FileInfo, byte[]> loadAll(Collection<FileInfo> keys) {
		HashMap<FileInfo, byte[]> res = new HashMap<>();
		for(FileInfo key : keys){
			res.put(key, load(key));
		}
		return res;
	}

	@Override
	public Iterable<FileInfo> loadAllKeys() {
		// not supported
		return null;
	}

}
