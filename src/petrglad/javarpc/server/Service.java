package petrglad.javarpc.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import petrglad.javarpc.Message;
import petrglad.javarpc.Response;

/**
 * Adapts RPC message to service implementation.
 */
public class Service {
    final private Object api;

    public Service(Object implementation) {
        this.api = implementation;
    }

    private Method getMethod(Message msg) throws SecurityException, NoSuchMethodException {
        List<Class<?>> argClasses = new ArrayList<Class<?>>();
        for (Object arg : msg.args) {
            argClasses.add(arg.getClass());
        }
        return api.getClass().getMethod(msg.methodName,
                argClasses.toArray(new Class[] {}));
    }

    /**
     * @return Response.
     */
    public Response process(Message msg) {
        final Method m;
        try {
            m = getMethod(msg);
        } catch (SecurityException e) {
            return new Response(msg, e);
        } catch (NoSuchMethodException e) {
            return new Response(msg, e);
        }
        try {
            return new Response(msg, m.invoke(api, msg.args.toArray()));
        } catch (InvocationTargetException e) {
            return new Response(msg, e.getCause());
        } catch (IllegalArgumentException e) {
            return new Response(msg, e);
        } catch (IllegalAccessException e) {
            return new Response(msg, e);
        }
    }
}
