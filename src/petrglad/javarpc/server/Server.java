package petrglad.javarpc.server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import petrglad.javarpc.util.Sockets;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class Server implements Runnable {

    static final Logger LOG = Logger.getLogger(Server.class);

    private final Services services;
    private final int port;
    private final ThreadPoolExecutor executor;

    public static void main(String[] args) {
        try {
            LOG.info("Starting server. Args: " + Arrays.toString(args));
            if (args.length < 2) {
                System.out.println("USAGE: Server services_config port_number");
            } else {
                Server s = new Server(new Services(loadConfig(args)), Integer.parseInt(args[1]));
                s.run();
            }
        } catch (Throwable e) {
            LOG.error("Caught exception at top level", e);
        }
    }

    private static Map<String, String> loadConfig(String[] args)
            throws IOException, FileNotFoundException {
        // XXX (refactoring) Better way to get String->String?
        Properties config = new Properties();
        config.load(new FileInputStream(args[0]));
        final Map<String, String> serviceConfig = new HashMap<>();
        for (Object key : config.keySet())
            serviceConfig.put((String) key, (String) config.get(key));
        return serviceConfig;
    }

    public Server(Services services, int port) {
        this.services = services;
        this.port = port;
        this.executor = new ThreadPoolExecutor(2, 16, 10, TimeUnit.SECONDS,
                // XXX Queue is unbounded.
                new LinkedBlockingQueue<>(),
                new ThreadFactoryBuilder()
                        .setNameFormat("server-executor-worker-%s")
                        .setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                            @Override
                            public void uncaughtException(Thread t, Throwable e) {
                                LOG.error("Caught exception in thread " + t.getName(), e);
                            }
                        })
                        .build());
        Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdown));
    }

    @Override
    public void run() {
        try {
            ServerSocket socket = new ServerSocket(port);
            try {
                LOG.info("Server is accepting connections on " + socket.getLocalSocketAddress());
                while (!socket.isClosed())
                    newConnection(socket.accept());
            } finally {
                Sockets.closeSocket(socket);
            }
        } catch (Exception e) {
            // TODO Log error here.
            throw new RuntimeException(e);
        }

    }

    private void newConnection(Socket socket) {
        LOG.info("Accepted client connection " + socket.getRemoteSocketAddress());
        new ServerSession(services, socket, executor);
    }
}
