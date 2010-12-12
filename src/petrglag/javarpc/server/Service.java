package petrglag.javarpc.server;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import petrglad.javarpc.Message;
import petrglad.javarpc.Response;

public class Service {
	final private Object api;

	public Service(Object implementation) {
		this.api = implementation;
	}

	/**
	 * @return Response.
	 */
	public Response process(Message msg) {
		List<Class<?>> argClasses = new ArrayList<Class<?>>();
		for (Object arg : msg.args) {
			argClasses.add(arg.getClass());
		}
		try {
			final Method m = api.getClass().getMethod(msg.methodName,
					argClasses.toArray(new Class[] {}));
			return new Response(msg, m.invoke(api, msg.args.toArray()));
		} catch (Exception e) {
			return new Response(msg, e);
		}
	}
}
