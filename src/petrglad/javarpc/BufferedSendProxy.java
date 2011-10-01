package petrglad.javarpc;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import petrglad.javarpc.util.Sink;
import petrglad.javarpc.util.Sockets;
import petrglad.javarpc.util.Spoolers;

/**
 * Common pattern of sending messages to socket via queue and handing out every
 * received message to a sink.
 * 
 * @param <T>
 */
public class BufferedSendProxy<T> implements Closeable {

    static final Logger LOG = Logger.getLogger(BufferedSendProxy.class);

    // XXX queue is unbounded
    private final BlockingQueue<T> sendQueue = new LinkedBlockingDeque<T>();

    private final Proxy<T> proxy;

    public BufferedSendProxy(Socket socket, Sink<Object> receivedSink) {
        proxy = new Proxy<T>(
                socket,
                receivedSink,
                Spoolers.bufferSupplier(sendQueue, Sockets.getIsSocketOpen(socket)));
    }

    public boolean send(T o) {
        while (proxy.isRunning.get())
            try {
                if (!sendQueue.offer(o, 2, TimeUnit.MINUTES))
                    break;

                return true;
            } catch (InterruptedException e) {
            }
        LOG.error("Can not send message " + o);
        return false;
    }

    @Override
    public void close() throws IOException {
        proxy.close();
    }
}
