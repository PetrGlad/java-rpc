package petrglad.javarpc.client;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import petrglad.javarpc.Message;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class Client {

    static final Logger LOG = Logger.getLogger(Client.class);

    final private ClientSession session;

    public static void main(String[] args) {
        try {
            if (args.length < 2) {
                System.out.println("Usage: Client host port");
            } else {
                LOG.info("Started " + Arrays.toString(args));
                final Client client = new Client(args[0], Integer.parseInt(args[1]));
                simpleTest(client);
                concurrentTest(client);
                LOG.info("Finished.");
            }
        } catch (Throwable e) {
            LOG.error("Caught exception at top level", e);
        }
    }

    private static void simpleTest(Client client) {
        {
            Long v = (Long) client.call("calculator.add", 3L, 4L);
            LOG.info("Add response " + v);
            assert v.equals(3L + 4L);
        }
        {
            Long v = (Long) client.call("calculator.guess", 3L, 4L);
            LOG.info("Guess response " + v);
            assert v.equals(3L * 4L);
        }
        {
            try {
                client.call("abnormal.blow");
                assert false : "Exception expected";
            } catch (RuntimeException e) {
                LOG.info("Blow exception " + e);
                assert "Never succeed".equals(e.getMessage());
            }
        }
    }

    /**
     * Starts several threads each sending a number of messages.
     */
    private static void concurrentTest(final Client client) {
        final Set<Thread> threads = Sets.newHashSet();
        for (int ti = 0; ti < 10; ti++) {
            final long threadNo = ti;
            final Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    final int N = 10000;
                    // request->expectedResult
                    final Map<Future<Object>, Long> requests = Maps.newHashMapWithExpectedSize(N);
                    for (long i = 0; i < N; i++) {
                        requests.put(client.send("calculator.guess", threadNo, i), threadNo * i);
                    }
                    while (!requests.isEmpty()) {

                        Iterator<Map.Entry<Future<Object>, Long>> i = requests.entrySet()
                                .iterator();
                        while (i.hasNext()) {
                            Entry<Future<Object>, Long> entry = i.next();
                            Future<Object> future = entry.getKey();
                            if (future.isDone()) {
                                try {
                                    Object v = future.get();
                                    if (v.equals(entry.getValue()))
                                        LOG.info("Request result=" + v);
                                    else
                                        LOG.error("Request result=" + v + ", expected result="
                                                + entry.getValue() + ", call=" + future);
                                } catch (InterruptedException e) {
                                    LOG.error("Request interrupted " + future);
                                } catch (ExecutionException e) {
                                    LOG.error("Request " + future + " threw an exception: "
                                            + e.getCause());
                                }
                                i.remove();
                            }
                        }
                    }
                }
            });
            threads.add(t);
            t.setName("Client " + ti);
            t.start();
        }
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
            }
        }
    }

    public Client(String host, int port) {
        this.session = new ClientSession(new Proxy<Message>(host, port));
    }

    /**
     * The API function suggested in the task.
     */
    public Object call(String methodName, Object... params) {
        try {
            return send(methodName, params).get();
        } catch (ExecutionException e) {
            throw (RuntimeException) e.getCause();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Future<Object> send(String methodName, Object... params) {
        return session.send(methodName, Arrays.asList(params));
    }
}
