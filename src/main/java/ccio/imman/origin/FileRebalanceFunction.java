package ccio.imman.origin;

import java.io.Serializable;
import java.util.concurrent.Callable;

import ccio.imman.FileInfo;

public class FileRebalanceFunction implements Callable<byte[]>, Serializable{

	private static final long serialVersionUID = 1L;
	
	private FileInfo fileInfo;

	public FileRebalanceFunction(FileInfo fileInfo) {
		super();
		this.fileInfo = fileInfo;
	}

	@Override
	public byte[] call() throws Exception {
		byte[] res = LocalFileLoader.loadFile(fileInfo);
		if(res != null){
			LocalFileLoader.deleteFile(fileInfo.canonicalPath());
		}
		return res;
	}

}
