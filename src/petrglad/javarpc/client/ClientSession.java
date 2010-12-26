package petrglad.javarpc.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import petrglad.javarpc.Message;
import petrglad.javarpc.Response;

/**
 * Client RPC session.
 */
public class ClientSession {

	private final AtomicLong messageSerialId = new AtomicLong(0);

	private final ServerProxy serverProxy;

	private final Map<Long, Response> responses = Collections
			.synchronizedMap(new HashMap<Long, Response>());

	public ClientSession(ServerProxy server) {
		this.serverProxy = server;
		serverProxy.open();
	}

	private Message newMessage(String target, List<Object> args) {
		assert !target.isEmpty();
		return new Message(messageSerialId.incrementAndGet(), target, args);
	}

	public long send(String methodName, List<Object> args) {
		final Message m = newMessage(methodName, args);
		serverProxy.send(m);
		responses.put(m.serialId, null);
		return m.serialId;
	}

	public Response getResponse(long messageId) {
		// (Otherwise we could poll asynchronously or give a callback to
		// serverProxy)
		receiveResponses();

		// Do not remove key if response is null (not received yet)
		Response r = responses.get(messageId);
		if (null != r) {
			return responses.remove(messageId);
		} else {
			return null;
		}
	}

	private void receiveResponses() {
		// TODO Detect timeouts here
		Response r = serverProxy.receive();
		if (null != r) {
			if (responses.containsKey(r.serialId))
				responses.put(r.serialId, r);
			else
				throw new RuntimeException("Got unrequested reponse: serialId="
						+ r.serialId);
		}
	}
}
