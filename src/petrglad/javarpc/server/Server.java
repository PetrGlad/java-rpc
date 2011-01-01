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

public class Server implements Runnable {

	static final Logger LOG = Logger.getLogger(Server.class);

	private final Services services;
	private final int port;
	private final ThreadPoolExecutor executor;

	public static void main(String[] args) throws FileNotFoundException,
			IOException {
		try {
			LOG.info("Starting server. Args: " + Arrays.toString(args));
			if (args.length < 2) {
				System.out.println("USAGE: Server services_config port_number");
			} else {
				Server s = new Server(new Services(loadConfig(args)),
						Integer.parseInt(args[1]));
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
		Map<String, String> serviceConfig = new HashMap<String, String>();
		for (Object key : config.keySet()) {
			serviceConfig.put((String) key, (String) config.get(key));
		}
		return serviceConfig;
	}

	public Server(Services services, int port) {
		this.services = services;
		this.port = port;
		// TODO Implement graceful shutdown.
		// XXX Queue is unbounded.
		this.executor = new ThreadPoolExecutor(2, 10, 10, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
	}

	@Override
	public void run() {
		try {
			ServerSocket socket = new ServerSocket(port);
			LOG.info("Server is accepting connections on "
					+ socket.getLocalPort());
			while (true) {
				newConnection(socket.accept());
			}
		} catch (Exception e) {
			// TODO Log error here.
			throw new RuntimeException(e);
		}
	}

	private void newConnection(Socket socket) {
		LOG.info("Accepted connection " + socket.getRemoteSocketAddress());
		Thread t = new Thread(new ServerSession(services, socket, executor));
		t.setDaemon(true);
		t.setName("Server session for " + socket.getRemoteSocketAddress());
		t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				LOG.error("Exception in session thread " + t.getName(), e);
			}
		});
		t.start();
	}
}
