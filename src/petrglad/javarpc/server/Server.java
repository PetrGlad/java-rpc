package petrglad.javarpc.server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Server implements Runnable {
	
	private final Services services;
	private final int port;

	// TODO Log exceptions
	public static void main(String[] args) throws FileNotFoundException,
			IOException {
		if (args.length < 2) {
			System.out.println("USAGE: Server services_config port_number");
		} else {
			Server s = new Server(new Services(loadConfig(args)),
					Integer.parseInt(args[1]));
			s.run();
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
	}

	@Override
	public void run() {
		try {			
			ServerSocket socket = new ServerSocket(port);
			System.out.println("Server is accepting connections on " + socket.getLocalPort());
			while (true) {
				newConnection(socket.accept());
			}
		} catch (Exception e) {
			// TODO Log error here.
			throw new RuntimeException(e);
		}
	}

	private void newConnection(Socket socket) {
		System.out.println("Accepted connection " + socket.getRemoteSocketAddress());
		Thread t = new Thread(new ServerSession(services, socket));
		t.setDaemon(true);
		t.setName("Server session for " + socket.getRemoteSocketAddress());
		t.start();
	}
}
