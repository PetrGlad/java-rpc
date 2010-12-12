package petrglag.javarpc.server;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import petrglad.javarpc.Message;
import petrglad.javarpc.Response;

public class ServerSession implements Runnable {

	private final Socket socket;
	private final Services services;

	private ObjectInputStream oIn = null;
	private ObjectOutputStream oOut = null;

	public ServerSession(Services services, Socket socket) {
		assert null != socket;
		assert null != services;
		this.socket = socket;
		this.services = services;
	}

	@Override
	public void run() {
		try {
			oIn = new ObjectInputStream(socket.getInputStream());
			oOut = new ObjectOutputStream(socket.getOutputStream());
			while (true) {
				Object o;
				try {
					o = oIn.readObject();
				} catch (EOFException e) {
					System.out.println("Client closed connection.");
					break;
				}
				if (o instanceof Message) {
					oOut.writeObject(process((Message) o));
				} else {
					throw new RuntimeException("Unexpected type of request "
							+ o.getClass().getCanonicalName());
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (null != oIn)
				try {
					oIn.close();
				} catch (IOException e) {
				}
			if (null != oOut)
				try {
					oOut.close();
				} catch (IOException e) {
				}
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}

	private Object process(Message msg) {
		String[] names = msg.methodName.split("\\.");
		Service s = services.get(names[0]);
		if (null == s) {
			return new Response(msg, new RuntimeException("Service " + names[0]
					+ " is not found."));
		}
		return s.process(new Message(msg.serialId, names[1], msg.args));
	}
}
