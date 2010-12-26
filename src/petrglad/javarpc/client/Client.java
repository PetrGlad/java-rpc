package petrglad.javarpc.client;

import java.util.Arrays;
import java.util.Set;

import org.apache.log4j.Logger;

import petrglad.javarpc.Response;

import com.google.common.collect.Sets;

public class Client {

	static final Logger LOG = Logger.getLogger(Client.class);

	final private ClientSession session;

	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage: Client host port");
		} else {
			LOG.info("Started " + Arrays.toString(args));
			Client client = new Client(args[0], Integer.parseInt(args[1]));
			// LOG.info("Response " + client.call("calculator.add", 3L, 4L));
			{
				int N = 100000;
				final Set<Long> tokens = Sets.newHashSetWithExpectedSize(N);
				for (long i = 0; i < N; i++)
					tokens.add(client.send("calculator.add", 3L, i));
				while (!tokens.isEmpty()) {
					Long id = tokens.iterator().next();
					Object result = client.receive(id);
					if (null != result) {
						LOG.info("Request id=" + id + ", result=" + result);				
						tokens.remove(id);
					}
					LOG.debug("Tokens count " + tokens.size());
				}
			}
			LOG.info("Finished.");
		}
	}

	public Client(String host, int port) {
		this.session = new ClientSession(new ServerProxy(host, port));
	}

	public Object call(String methodName, Object... params) {
		long messageId = send(methodName, params);
		// XXX Better to wait on condition instead of just polling. Asynchronous
		// call interface would be more natural choice here.
		while (true) {
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
			}
			Object result = receive(messageId);
			if (null != result)
				return result;
		}
	}

	private Object receive(long messageId) {
		Response response = session.getResponse(messageId);
		if (null != response) {
			RuntimeException e = response.asException();
			if (null != e) {
				throw e;
			} else {
				return response.value;
			}
		} else
			return null;
	}

	private long send(String methodName, Object... params) {
		return session.send(methodName, Arrays.asList(params));
	}
}
