package petrglad.javarpc.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import petrglad.javarpc.Utils;

import com.google.common.base.Supplier;
import com.google.common.io.Closeables;

public final class Spoolers {
    static final Logger LOG = Logger.getLogger(Spoolers.class);

    private Spoolers() {
    }

    public static Supplier<Object> socketReader(final Socket socket) {
        try {
            return new Supplier<Object>() {
                final ObjectInputStream oIn = new ObjectInputStream(
                        socket.getInputStream());

                @Override
                public Object get() {
                    try {
                        return oIn.readObject();
                    } catch (EOFException e) {
                        LOG.info("Remote side closed connection "
                                + socket.getRemoteSocketAddress());
                        return null;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> Sink<T> socketWriter(final Socket socket) {
        try {
            return new Sink<T>() {
                final ObjectOutputStream oOut = new ObjectOutputStream(
                        socket.getOutputStream());

                @Override
                public void put(Object o) {
                    try {
                        oOut.writeObject(o);
                        oOut.flush();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> Sink<T> bufferSink(final BlockingQueue<T> queue) {
        return new Sink<T>() {
            @Override
            public void put(T v) {
                // TODO loop until succeeded
                queue.offer(v);
            };
        };
    }

    public static <T> Supplier<T> bufferSupplier(final BlockingQueue<T> queue, final Flag isRunning) {
        return new Supplier<T>() {
            @Override
            public T get() {
                while (isRunning.get()) {
                    try {
                        T v = queue.poll(3, TimeUnit.SECONDS);
                        if (null != v)
                            return v;
                    } catch (InterruptedException e) {
                    }                    
                }
                return null;
            };
        };
    }

    public static void startThread(final String name, final Runnable proc) {
        final Thread t = new Thread(proc);
        t.setDaemon(true);
        t.setName(name);
        t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                LOG.error("Exception in spooler thread " + t.getName(), e);
            }
        });
        t.start();
    }

    /**
     * @return Stop condition for spoolers.
     */
    public static Flag getIsSocketOpen(final Socket socket) {
        return new Flag() {
            @Override
            public Boolean get() {
                return !socket.isClosed();
            }
        };
    }
}
