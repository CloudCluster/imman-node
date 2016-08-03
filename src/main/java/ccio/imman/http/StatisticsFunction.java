package ccio.imman.http;

import java.io.Serializable;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;

import ccio.imman.ImageServer;

public class StatisticsFunction implements Callable<String>, Serializable, HazelcastInstanceAware{

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsFunction.class);
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	private transient HazelcastInstance hzInstance;
	
	public StatisticsFunction(HazelcastInstance hzInstance) {
		super();
		this.hzInstance = hzInstance;
	}

	@Override
	public String call() throws Exception {
		LOGGER.debug("HZ Instance: {}", hzInstance);
		String res = MAPPER.writeValueAsString(hzInstance.getMap(ImageServer.NAME_MAP_IMAGES).getLocalMapStats());
		LOGGER.debug("Stat: {}", res);
		return res;
	}

	@Override
	public void setHazelcastInstance(HazelcastInstance hzInstance) {
		this.hzInstance = hzInstance;
	}
}
