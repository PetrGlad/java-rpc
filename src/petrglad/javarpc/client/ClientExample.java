package petrglad.javarpc.client;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import petrglad.javarpc.RpcException;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Example/test RPC client program.
 */
public class ClientExample {

    private static class Caller implements Runnable {
        private final int threadNo;
        private final Client client;

        private Caller(int threadNo, Client client) {
            this.threadNo = threadNo;
            this.client = client;
        }

        @Override
        public void run() {
            final int N = 10000;
            // request->expectedResult
            final Map<Future<Object>, Long> requests = Maps.newHashMapWithExpectedSize(N);
            for (long i = 0; i < N; i++)
                requests.put(client.send("calculator.guess", threadNo, i), threadNo * i);
            // Now wait for results
            while (!requests.isEmpty()) {
                Iterator<Map.Entry<Future<Object>, Long>> i = requests.entrySet().iterator();
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
    }

    static final Logger LOG = Logger.getLogger(ClientExample.class);

    public static void main(String[] args) {
        try {
            if (args.length < 2) {
                System.out.println("Usage: Client host port");
            } else {
                LOG.info("Started " + Arrays.toString(args));
                final Client client = new Client(args[0], Integer.parseInt(args[1]));
                basicTest(client);
                concurrentTest(client);
                client.close();
                LOG.info("Finished.");
            }
        } catch (Throwable e) {
            LOG.error("Caught exception at top level", e);
        }
    }

    private static void basicTest(Client client) {
        // XXX Use TestNG here?
        try {
            // No service
            client.call("notExistingService.add", 3L, 4L);
            assert false : "Exception expected";
        } catch (RpcException e) {
            assert e.getMessage().matches("Service.+notExistingService.+");
        }
        try {
            // No such method name
            client.call("calculator.notExistingMethod", 3L, 4L);
        } catch (RpcException e) {
            assert e.getMessage().matches("Method.+notExistingMethod.+");
        }
        try {
            // Number of parameters does not match
            client.call("calculator.add", 3L);
        } catch (RpcException e) {
            assert e.getMessage().matches("Method.+add.+");
        }
        try {
            // Types of parameters do not match
            client.call("calculator.add", "greeting", 1L);
        } catch (RpcException e) {
            assert e.getMessage().matches("Method.+add.+");
        }
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
        {
            // Method returning void.
            Object v = client.call("abnormal.voidMethod");
            assert v == null;
        }
        client.call("example.sleep", 123L);
        {
            Date v = (Date) client.call("example.getCurrentDate");
            assert (System.currentTimeMillis() - v.getTime()) < 1000;
        }
    }

    /**
     * Starts several threads each sending a number of messages.
     */
    private static void concurrentTest(final Client client) {
        final Set<Thread> threads = Sets.newHashSet();
        for (int ti = 0; ti < 10; ti++) {
            final Thread t = new Thread(new Caller(ti, client));
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
}
