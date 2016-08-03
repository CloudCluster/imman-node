package ccio.imman;

import java.io.Serializable;
import java.net.URLConnection;
import java.util.TreeSet;

import org.glassfish.grizzly.http.server.Request;

import com.google.common.net.MediaType;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;

import ccio.imman.config.DynamicIntSetProperty;

public class FileInfo implements Serializable{

	private static final long serialVersionUID = 1L;
	
	public static final DynamicIntSetProperty IMAGE_WIDTH=new DynamicIntSetProperty("image.width");
	public static final DynamicIntSetProperty IMAGE_HEIGHT=new DynamicIntSetProperty("image.height");
	
	private static final DynamicStringProperty IMAGE_WIDTH_PARAMETER = DynamicPropertyFactory.getInstance().getStringProperty("image.width.parameter", "iw");
	private static final DynamicStringProperty IMAGE_HEIGHT_PARAMETER = DynamicPropertyFactory.getInstance().getStringProperty("image.height.parameter", "ih");
	
	private String path;
	private Integer width;
	private Integer height;
	
	public FileInfo(){
		
	}
	
	public FileInfo(Request request){
		this.path = request.getPathInfo();
		this.width = getSupportedWidth(request.getParameter(IMAGE_WIDTH_PARAMETER.get()));
		this.height = getSupportedHeight(request.getParameter(IMAGE_HEIGHT_PARAMETER.get()));
	}
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Integer getWidth() {
		return width;
	}

	public void setWidth(Integer width) {
		this.width = width;
	}

	public Integer getHeight() {
		return height;
	}

	public void setHeight(Integer height) {
		this.height = height;
	}
	
	public boolean isOriginalFile() {
		return height == null && width == null;
	}
	
	public String canonicalPath() {
		StringBuilder sb = new StringBuilder(path);
		String div = "?";
		if(width != null){
			sb.append(div).append(IMAGE_WIDTH_PARAMETER.get()).append("=").append(width);
			div = "&";
		}
		if(height != null){
			sb.append(div).append(IMAGE_HEIGHT_PARAMETER.get()).append("=").append(height);
		}
		return sb.toString();
	}
	
	public int cacheKeyFinalFile(){
		return canonicalPath().hashCode();
	}
	
	public int cacheKeyOriginalFile(){
		return path.hashCode();
	}
	
	public int getHostNumber(int hostsCount){
		if(hostsCount==0){
			return 0;
		}
		return Math.abs(canonicalPath().hashCode() % hostsCount);
	}
	
	public String contentType(){
		String contentType = URLConnection.guessContentTypeFromName(path);
		if (contentType == null) {
			contentType = MediaType.JPEG.toString();
		}
		return contentType;
	}
	
	private Integer getSupportedWidth(Integer iw){
		if(iw==null || IMAGE_WIDTH.get()==null || IMAGE_WIDTH.get().size()==0){
			return null;
		}else{
			TreeSet<Integer> imgWidth=new TreeSet<Integer>();
			for(Integer i:IMAGE_WIDTH.get()){
				if(i!=null){
					imgWidth.add(i);
				}
			}
			Integer res=imgWidth.ceiling(iw);
			if(res==null){
				return imgWidth.last();
			}else{
				return res;
			}
		}
	}
	
	private Integer getSupportedWidth(String iw){
		if(iw!=null){
			try{
				return getSupportedWidth(Integer.valueOf(iw));
			}catch(NumberFormatException e){
				//ignore
			}
		}
		return null;
	}
	
	private Integer getSupportedHeight(Integer ih){
		if(ih==null || IMAGE_HEIGHT.get()==null || IMAGE_HEIGHT.get().size()==0){
			return null;
		}else{
			TreeSet<Integer> imgHeight=new TreeSet<Integer>();
			for(Integer i:IMAGE_HEIGHT.get()){
				if(i!=null){
					imgHeight.add(i);
				}
			}
			Integer res=imgHeight.ceiling(ih);
			if(res==null){
				return imgHeight.last();
			}else{
				return res;
			}
		}
	}
	
	private Integer getSupportedHeight(String ih){
		if(ih!=null){
			try{
				return getSupportedHeight(Integer.valueOf(ih));
			}catch(NumberFormatException e){
				//ignore
			}
		}
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((height == null) ? 0 : height.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((width == null) ? 0 : width.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileInfo other = (FileInfo) obj;
		if (height == null) {
			if (other.height != null)
				return false;
		} else if (!height.equals(other.height))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (width == null) {
			if (other.width != null)
				return false;
		} else if (!width.equals(other.width))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "FileInfo [path=" + path + ", width=" + width + ", height=" + height + "]";
	}

}