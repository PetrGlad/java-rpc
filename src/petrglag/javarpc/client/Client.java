package petrglag.javarpc.client;

import java.util.Arrays;

import petrglag.javarpc.Response;

public class Client {
	ClientSession session;
		
	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage: Client host port");
		} else {
			Client client = new Client(args[0], Integer.parseInt(args[1]));
			System.out.println("Response " + client.call("calculator.add", 3L, 4L));
		}		
	}

	public Client(String host, int port) {
		this.session = new ClientSession(new ServerProxy(host, port));
	}

	public Object call(String methodName, Object... params) {
		long messageId = session.send(methodName, Arrays.asList(params));		
		// XXX Better to wait on condition instead of just polling. 
		while (true) {
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
			}
			Response response = session.getResponse(messageId);
			if (null != response)
			{
				RuntimeException e = response.asException();
				if (null != e) {
					throw e;
				} else {
					return response.value;
				}
			}
		}
	}
}
