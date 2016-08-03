package ccio.imman.config;

import java.util.Set;

import com.netflix.config.DynamicSetProperty;

public class DynamicIntSetProperty extends DynamicSetProperty<Integer>{

	public DynamicIntSetProperty(String propName) {
		super(propName, (Set<Integer>)null);
	}

	@Override
	protected Integer from(String value) {
		if(value==null){
			return null;
		}
		try{
			return Integer.valueOf(value);
		}catch(NumberFormatException e){
			return null;
		}
	}

}
