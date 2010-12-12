package petrglad.javarpc.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import petrglad.javarpc.Message;
import petrglad.javarpc.Response;

/**
 * Example taken from here:
 * http://download.oracle.com/javase/tutorial/networking
 * /sockets/examples/KnockKnockClient.java
 * 
 * @author petr
 */
public class ServerProxy {

	private final String host;
	private final int port;

	private Socket kkSocket = null;
	private ObjectOutputStream oOut = null;
	private ObjectInputStream oIn = null;

	public ServerProxy(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public void open() {
		assert null == kkSocket;
		assert null == oOut;
		assert null == oIn;
		try {
			kkSocket = new Socket(host, port);

			OutputStream out = kkSocket.getOutputStream();
			oOut = new ObjectOutputStream(out);

			InputStream in = kkSocket.getInputStream();
			oIn = new ObjectInputStream(in);
		} catch (UnknownHostException e) {
			throw new RuntimeException("Host is not found " + host + ":" + port);
		} catch (IOException e) {
			throw new RuntimeException(
					"Could not open connection to " + host);
		}
	}

	public void send(Message m) {
		try {
			oOut.writeObject(m);
			oOut.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Response receive() {
		try {
			Object o = oIn.readObject();
			if (o instanceof Response) {
				return (Response) o;
			} else {
				throw new RuntimeException(
						"Reponse object is of unexpected type "
								+ o.getClass().getCanonicalName());
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void close() {
		try {
			oOut.close();
		} catch (IOException e) {
		}
		try {
			oIn.close();
		} catch (IOException e) {
		}
		try {
			kkSocket.close();
		} catch (IOException e) {
		}
	}
}
