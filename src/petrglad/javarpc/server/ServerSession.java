package petrglad.javarpc.server;

import java.net.Socket;
import java.util.concurrent.Executor;

import org.apache.log4j.Logger;

import petrglad.javarpc.Message;
import petrglad.javarpc.Response;
import petrglad.javarpc.RpcException;
import petrglad.javarpc.client.BufferedSendProxy;
import petrglad.javarpc.util.Sink;

public class ServerSession {
    static final Logger LOG = Logger.getLogger(Server.class);

    private final Services services;
    private final Executor executor;
    private final BufferedSendProxy<Response> clientProxy;

    public ServerSession(Services services, Socket socket, Executor executor) {
        assert null != services;
        this.services = services;
        this.executor = executor;
        this.clientProxy = new BufferedSendProxy<Response>(
                socket,
                // Receives messages from client
                new Sink<Object>() {
                    @Override
                    public void put(Object v) {
                        process(v);
                    }
                });
    }

    /**
     * Dispatch message object to appropriate service or send error result
     * immediately if it can not be dispatched.
     */
    private void process(final Object o) {
        if (!(o instanceof Message))
            // XXX Write some error response here?
            throw new RuntimeException("Unexpected type of request "
                    + o.getClass().getCanonicalName());
        final Message msg = (Message) o;
        final String[] names = msg.methodName.split("\\.");
        final Service s = services.get(names[0]);
        if (null == s) {
            clientProxy.send(new Response(msg, new RpcException("Service " + names[0]
                    + " is not found.")));
        } else {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        clientProxy.send(s.process(new Message(msg.serialId, names[1], msg.args)));
                    } catch (Exception e) {
                        clientProxy.send(new Response(msg, e));
                    }
                }
            });
        }
    }
}
