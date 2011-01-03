package petrglad.javarpc.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import petrglad.javarpc.Utils;
import petrglad.javarpc.server.ServerSession;
import petrglad.javarpc.util.Sink;
import petrglad.javarpc.util.Spooler;
import petrglad.javarpc.util.Spoolers;

import com.google.common.base.Supplier;

/**
 * Handles connection to server and message serialization.
 * 
 * <p>
 * XXX (refactoring) Actually it is possible extract similarities between this
 * and {@link ServerSession} (in particular this would require to get out socket
 * initialization). But common code is rather small (starting spoolers).
 * 
 * @author petr
 */
public class Proxy<T> implements Closeable {

    private final String host;
    private final int port;

    private Socket socket = null;
    Sink<Object> receivedSink;
    Supplier<T> sendSource;

    public Proxy(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Part of initialization procedure.
     */
    public void attach(final Sink<Object> receivedSink,
            final Supplier<T> sendSource) {
        this.receivedSink = receivedSink;
        this.sendSource = sendSource;
    }

    public void open() {
        assert null == socket;
        assert null != receivedSink && null != sendSource;
        try {
            socket = new Socket(host, port);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Host is not found " + host + ":" + port);
        } catch (IOException e) {
            throw new RuntimeException("Could not open connection to " + host);
        }
        Spoolers.startThread("Client reader", //
                new Spooler<Object>( //
                        Spoolers.socketReader(socket), //
                        receivedSink, //
                        Spoolers.getIsSocketOpen(socket)));
        Spoolers.startThread("Client sender", //
                new Spooler<T>( //
                        sendSource, //
                        Spoolers.<T> socketWriter(socket), //
                        Spoolers.getIsSocketOpen(socket)));
    }

    @Override
    public void close() {
        Utils.closeSocket(socket);
    }
}
