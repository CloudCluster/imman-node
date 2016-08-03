package ccio.imman.manipulation;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ccio.imman.FileInfo;
import ccio.imman.Manipulation;
import net.coobird.thumbnailator.Thumbnails;

public class ThumbnailatorManipulation implements Manipulation {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ThumbnailatorManipulation.class);

	@Override
	public byte[] manipulate(byte[] originalImage, FileInfo fileInfo) {
		return resizeImage(fileInfo.getWidth(), fileInfo.getHeight(), fileInfo.contentType(), originalImage);
	}

	private byte[] resizeImage(Integer newWidth, Integer newHeight, String mimeType, byte[] bytes) {
		BufferedImage image;
		try{
			image=ImageIO.read(new ByteArrayInputStream(bytes));
		}catch(IOException e){
			LOGGER.debug(e.getMessage(), e);
			return null;
		}
		int origWidth=image.getWidth();
		int origHeight=image.getHeight();

		if(newWidth==null || origWidth<newWidth){
			if(newHeight==null || newHeight<=0){
				newWidth=origWidth;
			}else{
				newWidth=origWidth*newHeight/origHeight;
			}
		}
		if(newHeight==null || origHeight<newHeight){
			newHeight=origHeight*newWidth/origWidth;
		}
		
		String format=mimeType.split("/")[1];
		//image.getType() for the type?
		ByteArrayOutputStream bous=new ByteArrayOutputStream();
		try {
			Thumbnails.of(image).size(newWidth, newHeight).imageType(BufferedImage.TYPE_INT_ARGB).outputFormat(format).toOutputStream(bous);
		} catch (IllegalArgumentException | IOException e) {
			LOGGER.debug(e.getMessage(), e);
			return null;
		}
		return bous.toByteArray();
	}

}
