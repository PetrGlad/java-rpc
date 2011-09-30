package petrglad.javarpc.client;

import java.io.Closeable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import petrglad.javarpc.Message;
import petrglad.javarpc.Response;
import petrglad.javarpc.util.Flag;
import petrglad.javarpc.util.Sink;
import petrglad.javarpc.util.Spoolers;

/**
 * Client-side RPC session.
 */
public class ClientSession implements Closeable {

    static final Logger LOG = Logger.getLogger(ClientSession.class);

    class Result implements Future<Object> {
        private Response response;
        private final Object responseChange = new Object();
        private final long requestId;

        public Result(long requestId) {
            this.requestId = requestId;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false; // Is not supported
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return response != null;
        }

        @Override
        public Object get() throws InterruptedException, ExecutionException {
            while (true) {
                try {
                    return get(10, TimeUnit.DAYS);
                } catch (TimeoutException e) {
                    // ignored
                }
            }
        }

        @Override
        public Object get(long timeout, TimeUnit unit) throws InterruptedException,
                ExecutionException, TimeoutException {
            if (isDone())
                return getNow();
            synchronized (responseChange) {
                responseChange.wait(unit.toMillis(timeout));
            }
            if (isDone())
                return getNow();
            else
                throw new TimeoutException("Waiting for result #" + response.serialId
                        + " timed out");
        }

        private Object getNow() throws ExecutionException {
            RuntimeException e = response.asException();
            if (null != e)
                throw new ExecutionException(e);
            else
                return response.value;
        }

        public void setResponse(Response response) {
            assert this.response == null;
            this.response = response;
            synchronized (responseChange) {
                responseChange.notifyAll();
            }
        }

        @Override
        public String toString() {
            return "Result [requestId=" + requestId + ", response=" + response + "]";
        }
    };

    private final AtomicLong messageSerialId = new AtomicLong(0);

    private final Proxy<Message> serverProxy;

    private final BlockingQueue<Message> sendQueue = new LinkedBlockingDeque<Message>();

    private final Map<Long, Result> results = Collections
            .synchronizedMap(new HashMap<Long, Result>());
    boolean isClosed = false;

    public ClientSession(Proxy<Message> server) {
        serverProxy = server;
        serverProxy.attach(
                // Receives messages from server:
                new Sink<Object>() {
                    @Override
                    public void put(Object v) {
                        final Response r = (Response) v;
                        Result result = results.remove(r.serialId);
                        if (result == null)
                            LOG.error("Response with id " + r.serialId + " is not expected.");
                        else
                            result.setResponse(r);
                    }
                },
                // Provides messages to be sent:
                Spoolers.bufferSupplier(sendQueue,
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

    private Message newMessage(String qualifiedMethodName, List<Object> args) {        
        return new Message(messageSerialId.incrementAndGet(), qualifiedMethodName, args);
    }

    public Future<Object> send(String qualifiedMethodName, List<Object> args) {
        final Message m = newMessage(qualifiedMethodName, args);
        final Result result = new Result(m.serialId);
        results.put(m.serialId, result);
        sendQueue.offer(m);
        return result;
    }
}
