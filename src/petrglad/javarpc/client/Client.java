package petrglad.javarpc.client;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import petrglad.javarpc.util.Sockets;

/**
 * Convenience wrapper for ClientSession. Adds synchronous call method.
 */
public class Client implements Closeable {

    final private ClientSession session;

    public Client(String host, int port) {
        this.session = new ClientSession(Sockets.openClientSocket(host, port));
    }

    /**
     * The API function suggested in the task description (prefer using futures
     * instead).
     */
    public Object call(String methodName, Object... params) {
        try {
            final Future<Object> result = send(methodName, params);
            if (result == null)
                throw new RuntimeException("Can not send request.");
            else
                return result.get();
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
