package petrglad.javarpc.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import petrglad.javarpc.Message;
import petrglad.javarpc.Response;
import petrglad.javarpc.RpcException;

/**
 * Adapts RPC message to service implementation.
 */
public class Service {
    final private Object api;

    public Service(Object implementation) {
        this.api = implementation;
    }

    public List<Class<?>> getArgTypes(Message msg) {
        List<Class<?>> argClasses = new ArrayList<Class<?>>();
        for (Object arg : msg.args)
            argClasses.add(arg.getClass());
        return argClasses;
    }

    /**
     * @return Response.
     */
    public Response process(Message msg) {
        final Method m;
        final List<Class<?>> argTypes = getArgTypes(msg);
        try {
            m = api.getClass().getMethod(msg.methodName, argTypes.toArray(new Class[] {}));
        } catch (SecurityException e) {
            return new Response(msg, e);
        } catch (NoSuchMethodException e) {
            return new Response(msg,
                    new RpcException("Method " + msg.methodName + " with parameters " + argTypes
                            + " is not found.", e));
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
