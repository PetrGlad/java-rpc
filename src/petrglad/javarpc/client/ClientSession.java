package petrglad.javarpc.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

import petrglad.javarpc.Message;
import petrglad.javarpc.Response;

/**
 * Client RPC session.  
 */
public class ClientSession {

    private final AtomicLong messageSerialId = new AtomicLong(0);

    private final ServerProxy serverProxy;

    private final BlockingQueue<Message> sendQueue = new LinkedBlockingDeque<Message>();

    private final Map<Long, Response> responses = Collections
            .synchronizedMap(new HashMap<Long, Response>());

    private boolean isOpen;

    public ClientSession(ServerProxy server) {
        serverProxy = server;
        isOpen = true;
        serverProxy.open();
        startReceiver();
        startSender();
    }

    public void close() {
        isOpen = false;
        serverProxy.close();
    }

    private void startSpooler(final String name, final Runnable proc) {
        Thread sender = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isOpen)
                    proc.run();
            }
        });
        sender.setName(name);
        sender.setDaemon(true);
        sender.start();
    }

    private void startReceiver() {
        startSpooler("Client receiver", new Runnable() {
            @Override
            public void run() {
                receiveResponses();
            }
        });
    }

    private void startSender() {
        startSpooler("Client sender", new Runnable() {
            @Override
            public void run() {
                sendResponses();
            }
        });
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

    private void receiveResponses() {
        // TODO Detect timeouts here?
        final Response r = serverProxy.receive();
        responses.put(r.serialId, r);
    }

    private void sendResponses() {
        while (!sendQueue.isEmpty()) {
            final Message m = sendQueue.poll();
            if (null != m)
                serverProxy.send(m);
        }
    }
}
