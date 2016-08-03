package ccio.imman.origin;

import java.io.IOException;
import java.net.UnknownHostException;

import org.apache.http.NoHttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;

import ccio.imman.FileInfo;

public class AwsS3Loader extends AbstractMapLoader{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AwsS3Loader.class);
	
	private static final DynamicStringProperty AWS_ACCESS_KEY = DynamicPropertyFactory.getInstance().getStringProperty("aws.s3.access", System.getProperty("aws.s3.access"));
	private static final DynamicStringProperty AWS_SECRET = DynamicPropertyFactory.getInstance().getStringProperty("aws.s3.secret", System.getProperty("aws.s3.secret"));
	private static final DynamicStringProperty AWS_BUCKET = DynamicPropertyFactory.getInstance().getStringProperty("aws.s3.bucket", System.getProperty("aws.s3.bucket"));
	
	private AmazonS3 amazonS3;

	public AwsS3Loader() {
		if (AWS_ACCESS_KEY.get() != null && AWS_SECRET.get() != null) {
			ClientConfiguration config = new ClientConfiguration();
			config.setMaxConnections(100);
			amazonS3 = new AmazonS3Client(new BasicAWSCredentials(AWS_ACCESS_KEY.get(), AWS_SECRET.get()), config);
		}
	}

	@Override
	public byte[] load(FileInfo fileInfo) {
		byte[] bytes = null;
		try{
			S3Object s3o = null;
			String s3key = fileInfo.getPath();
			try {
				if (s3key.startsWith("/")) {
					s3key = s3key.substring(1);
				}
	
				LOGGER.debug("S3 Call: {}", s3key);
				s3o = amazonS3.getObject(AWS_BUCKET.get(), s3key);
				bytes = IOUtils.toByteArray(s3o.getObjectContent());
				LOGGER.debug("S3 Call END");
	
			} catch (AmazonClientException | NoHttpResponseException | UnknownHostException e) {
				LOGGER.debug("Failed getting file from S3", e);
			} finally {
				if (s3o != null) {
					s3o.close();
				}
			}
		} catch (IOException e) {
			LOGGER.debug("Failed in AWS S3", e);
		}
		
		return bytes;
	}
}
