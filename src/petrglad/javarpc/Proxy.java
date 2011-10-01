package petrglad.javarpc;

import java.io.Closeable;
import java.net.Socket;

import petrglad.javarpc.util.Flag;
import petrglad.javarpc.util.Sink;
import petrglad.javarpc.util.Sockets;
import petrglad.javarpc.util.Spooler;
import petrglad.javarpc.util.Spoolers;

import com.google.common.base.Supplier;

/**
 * Handles connection to server and message serialization.
 * 
 * @param <T>
 *            type of messages that this proxy sends.
 */
public class Proxy<T> implements Closeable {

    private final Socket socket;

    public final Flag isRunning;

    public Proxy(Socket socket, final Sink<Object> receivedSink,
            final Supplier<T> sendSource) {
        assert null != socket;
        assert socket.isBound();
        this.socket = socket;
        isRunning = Sockets.getIsSocketOpen(socket);
        Spoolers.startThread("Socket " + socket.getInetAddress() + " sender",
                new Spooler<T>(sendSource, Spoolers.<T> socketWriter(socket), isRunning));
        Spoolers.startThread("Socket " + socket.getInetAddress() + " reader",
                new Spooler<Object>(Spoolers.socketReader(socket), receivedSink, isRunning));
    }

    @Override
    public void close() {
        Sockets.closeSocket(socket);
    }
}
