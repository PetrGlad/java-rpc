package petrglad.javarpc.server;

import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import petrglad.javarpc.Message;
import petrglad.javarpc.Response;
import petrglad.javarpc.RpcException;
import petrglad.javarpc.util.Flag;
import petrglad.javarpc.util.Sink;
import petrglad.javarpc.util.Spooler;
import petrglad.javarpc.util.Spoolers;

public class ServerSession {
    static final Logger LOG = Logger.getLogger(Server.class);

    private final Socket socket;
    private final Services services;
    private final Executor executor;

    // XXX queue is unbounded
    private final BlockingQueue<Response> completedCalls = new LinkedBlockingQueue<Response>();

    public ServerSession(Services services, Socket socket, Executor executor) {
        assert null != socket;
        assert null != services;
        assert socket.isBound();
        this.socket = socket;
        this.services = services;
        this.executor = executor;
    }

    public void open() {
        final Flag isRunning = Spoolers.getIsSocketOpen(socket);
        Spoolers.startThread("Response writer", new Spooler<Response>( //
                Spoolers.bufferSupplier(completedCalls, isRunning), //
                Spoolers.<Response> socketWriter(socket), //
                isRunning));
        Spoolers.startThread("Request reader", new Spooler<Object>( //
                Spoolers.socketReader(socket), //
                new Sink<Object>() {
                    @Override
                    public void put(Object v) {
                        process(v);
                    }
                },
                isRunning));
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
            enqueueResult(new Response(msg, new RpcException("Service " + names[0]
                    + " is not found.")));
        } else {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        enqueueResult(s.process(new Message(msg.serialId, names[1], msg.args)));
                    } catch (Exception e) {
                        enqueueResult(new Response(msg, e));
                    }
                }
            });
        }
    }

    void enqueueResult(Response response) {
        while (!socket.isClosed())
            try {
                if (!completedCalls.offer(response, 2, TimeUnit.MINUTES))
                    LOG.error("Can not send result (response queue is full) responseId="
                            + response.serialId);
                return;
            } catch (InterruptedException e) {
            }
    }
}
