package petrglad.javarpc.server;

import java.util.HashMap;
import java.util.Map;

public class Services {
	Map<String, Service> dispatch = new HashMap<String, Service>();  
	
	public Services(Map<String, String> mapping) {
		initialize(mapping);
	}

	private void initialize(Map<String, String> mapping) {
		for (Map.Entry<String, String> entry : mapping.entrySet()) {
			dispatch.put(entry.getKey(), newServiceInstance(entry.getValue()));
		}
	}

	private Service newServiceInstance(String className) {		
		try {
			return new Service(getClass().getClassLoader().loadClass(className).newInstance());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public Service get(String serviceName) {
		return dispatch.get(serviceName);
	}
}
