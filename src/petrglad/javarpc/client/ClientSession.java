package petrglad.javarpc.client;

import java.io.Closeable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

import petrglad.javarpc.Message;
import petrglad.javarpc.Response;
import petrglad.javarpc.util.Flag;
import petrglad.javarpc.util.Sink;
import petrglad.javarpc.util.Spoolers;

/**
 * Client RPC session.
 */
public class ClientSession implements Closeable {

    private final AtomicLong messageSerialId = new AtomicLong(0);

    private final Proxy<Message> serverProxy;

    private final BlockingQueue<Message> sendQueue = new LinkedBlockingDeque<Message>();

    private final Map<Long, Response> responses = Collections
            .synchronizedMap(new HashMap<Long, Response>());
    boolean isClosed = false;

    public ClientSession(Proxy<Message> server) {
        serverProxy = server;
        serverProxy.attach(newReceiver(), //
                Spoolers.bufferSupplier(sendQueue, //
                        new Flag() {
                            @Override
                            public Boolean get() {
                                return !isClosed;
                            }
                        }));
        serverProxy.open();
    }

    @Override
    public void close() {
        isClosed = true;
        serverProxy.close();
    }

    private Sink<Object> newReceiver() {
        return new Sink<Object>() {
            @Override
            public void put(Object v) {
                final Response r = (Response) v;
                responses.put(r.serialId, r);
            }
        };
    }

    private Message newMessage(String target, List<Object> args) {
        assert !target.isEmpty();
        return new Message(messageSerialId.incrementAndGet(), target, args);
    }

    public long send(String methodName, List<Object> args) {
        final Message m = newMessage(methodName, args);
        sendQueue.offer(m);
        return m.serialId;
    }

    public Response getResponse(long messageId) {
        return responses.remove(messageId);
    }
}
