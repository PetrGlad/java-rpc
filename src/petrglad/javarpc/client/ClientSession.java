package petrglad.javarpc.client;

import org.apache.log4j.Logger;
import petrglad.javarpc.BufferedSendProxy;
import petrglad.javarpc.Message;
import petrglad.javarpc.Response;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Client-side RPC session.
 */
public class ClientSession implements Closeable {

    static final Logger LOG = Logger.getLogger(ClientSession.class);

    static class Result extends CompletableFuture<Object> {
        final long requestId;

        public Result(long requestId) {
            this.requestId = requestId;
        }

        @Override
        public String toString() {
            return "Result [requestId=" + requestId + ", response=" + getNow(null) + "]";
        }
    }

    private final AtomicLong messageSerialId = new AtomicLong(0);
    private final BufferedSendProxy<Message> serverProxy;
    private final Map<Long, Result> results = new ConcurrentHashMap<>();

    public ClientSession(Socket socket) {
        serverProxy = new BufferedSendProxy<>(socket,
                // Receives messages from server
                v -> {
                    final Response r = (Response) v; // TODO Get rid of this cast
                    final Result result = results.remove(r.serialId);
                    if (result == null) {
                        LOG.error("Response with id " + r.serialId + " is not expected.");
                    } else {
                        final Throwable t = r.asException();
                        if (t == null) {
                            result.complete(r.value);
                        } else {
                            result.completeExceptionally(t);
                        }
                    }
                });
    }

    @Override
    public void close() throws IOException {
        serverProxy.close();
    }

    private Message newMessage(String qualifiedMethodName, List<Object> args) {
        return new Message(messageSerialId.incrementAndGet(), qualifiedMethodName, args);
    }

    /**
     * @return Future that provides result of remote method invocation, or null
     * if there was an error.
     */
    public CompletableFuture<Object> send(String qualifiedMethodName, List<Object> args) {
        final Message m = newMessage(qualifiedMethodName, args);
        final Result result = new Result(m.serialId);
        results.put(m.serialId, result);
        if (serverProxy.send(m))
            return result;
        else {
            results.remove(m.serialId);
            return null;
        }
    }
}
