package petrglad.javarpc.server;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import petrglad.javarpc.Message;
import petrglad.javarpc.Response;

public class ServerSession implements Runnable {
	static final Logger LOG = Logger.getLogger(Server.class);

	private final Socket socket;
	private final Services services;
	private final Executor executor;
	private Thread responseWriter;

	// XXX queue is unbounded
	private BlockingQueue<Response> completedCalls = new LinkedBlockingQueue<Response>();

	public ServerSession(Services services, Socket socket, Executor executor) {
		assert null != socket;
		assert null != services;
		this.socket = socket;
		this.services = services;
		this.executor = executor;
	}

	@Override
	public void run() {
		startResponseWriter();

		ObjectInputStream oIn = null;
		try {
			oIn = new ObjectInputStream(socket.getInputStream());
			while (true) {
				Object o;
				try {
					o = oIn.readObject();
				} catch (EOFException e) {
					LOG.info("Client closed connection.", e);
					break;
				}
				if (o instanceof Message) {
					process((Message) o);
				} else {
					// XXX Write response here?
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
			closeSocket();
		}
	}

	private void startResponseWriter() {
		// XXX Two threads per client might be too involved. Alternatively we
		// could have mutex for response write and write responses directly from
		// all tasks but that would block executor threads if there are many
		// concurrent responses to same client.
		responseWriter = new Thread(new Runnable() {
			@Override
			public void run() {
				ObjectOutputStream oOut = null;
				try {
					oOut = new ObjectOutputStream(socket.getOutputStream());
					while (true) {
						final Response r = completedCalls.poll(3,
								TimeUnit.SECONDS);
						if (null != r)
							oOut.writeObject(r);
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					if (null != oOut)
						try {
							oOut.close();
						} catch (IOException e) {
						}
					closeSocket();
				}
			}
		});
		responseWriter.setDaemon(true);
		responseWriter.start();
	}

	private void closeSocket() {
		try {
			socket.close();
		} catch (IOException e) {
		}
	}

	private void process(final Message msg) {
		final String[] names = msg.methodName.split("\\.");
		final Service s = services.get(names[0]);
		if (null == s) {
			enqueueResult(new Response(msg, new RuntimeException("Service "
					+ names[0] + " is not found.")));
		}
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					enqueueResult(s.process(new Message(msg.serialId, names[1],
							msg.args)));
				} catch (Exception e) {
					enqueueResult(new Response(msg, e));
				}
			}
		});
	}

	void enqueueResult(Response response) {
		while (true)
			try {
				if (!completedCalls.offer(response, 2, TimeUnit.MINUTES))
					LOG.error("Can not send result (response queue is full) responseId="
							+ response.serialId);
				return;
			} catch (InterruptedException e) {
			}
	}
}
