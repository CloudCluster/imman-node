package ccio.imman;

import java.util.List;

public class ClusterUtil {
	
	public static String findNode(FileInfo fileInfo){
		List<String> seeds = ImageServer.NODES.get();
		return seeds.get(Math.abs(fileInfo.canonicalPath().hashCode() % seeds.size()));
	}
	
	public static boolean isMe(String nodeIp){
		return ImageServer.SERVER_IP_PRIVATE.get().equals(nodeIp);
	}
}
