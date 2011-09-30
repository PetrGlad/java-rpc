package petrglad.javarpc.client;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import petrglad.javarpc.Message;

/**
 * Convenience wrapper for ClientSession. Adds synchronous call method.
 */
public class Client implements Closeable {

    final private ClientSession session;

    public Client(String host, int port) {
        this.session = new ClientSession(new Proxy<Message>(host, port));
    }

    /**
     * The API function suggested in the task description (prefer using futures
     * instead).
     */
    public Object call(String methodName, Object... params) {
        try {
            return send(methodName, params).get();
        } catch (ExecutionException e) {
            throw (RuntimeException) e.getCause();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Future<Object> send(String qualifiedMethodName, Object... params) {
        return session.send(qualifiedMethodName, Arrays.asList(params));
    }

    public Future<Object> send(String serviceName, String methodName, Object... params) {
        return session.send(serviceName + "." + methodName, Arrays.asList(params));
    }

    @Override
    public void close() throws IOException {
        session.close();
    }
}
