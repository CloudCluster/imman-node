package ccio.imman.http;

import org.glassfish.grizzly.WriteHandler;
import org.glassfish.grizzly.http.io.NIOOutputStream;
import org.glassfish.grizzly.http.server.Response;

import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;

public abstract class BytesWriteHandler implements WriteHandler{

	private static final DynamicIntProperty WRITE_CHUNK_SIZE = DynamicPropertyFactory.getInstance().getIntProperty("http.chunk.size.write", 8192);;

	private byte[] bytes;
	private NIOOutputStream out;
	private Response response;
	private int off = 0;
	
	public BytesWriteHandler(byte[] bytes, Response resp, NIOOutputStream out) {
		this.bytes = bytes;
		this.response = resp;
		this.out = out;
	}
	
	@Override
	public void onWritePossible() throws Exception {
		int len = WRITE_CHUNK_SIZE.get();
		if(len > bytes.length - off){
			len = bytes.length - off;
		}
		
		out.write(bytes, off, len);
		off += len;
        if (off < bytes.length) {
        	out.notifyCanWrite(this);
        } else {
        	out.close();
        	if (response.isSuspended()) {
        		response.resume();
        	} else {
        		response.finish();                   
        	}
        }
	}

}
