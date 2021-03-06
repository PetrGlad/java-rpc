package petrglad.javarpc.client;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.omg.CORBA.StringSeqHolder;
import petrglad.javarpc.RpcException;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

            final Collection<CompletableFuture<Void>> requests = new ArrayList<>(N);
            for (long i = 0; i < N; i++) {
                CompletableFuture<Object> result = client.send("calculator.guess", (long) threadNo, i);
                if (result == null) {
                    LOG.error("Can not send request t=" + threadNo + ", i=" + i);
                } else {
                    final long expectedResult = threadNo * i;
                    requests.add(result.handleAsync((v, throwable) -> {
                        if (throwable != null) {
                            LOG.error("Request " + result + " threw an exception: " + throwable);
                        } else {
                            if (v.equals(expectedResult))
                                LOG.info("Request result=" + v);
                            else
                                LOG.error("Request result=" + v + ", expected result="
                                        + expectedResult + ", call=" + result);
                        }
                        return null;
                    }));
                }
            }

            // Now wait for results
            requests.forEach(fut -> {
                try {
                    fut.get();
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("Request " + fut + " threw an exception: " + e);
                }
            });
        }
    }

    static final Logger LOG = Logger.getLogger(ClientExample.class);

    public static void main(String[] args) {
        try {
            if (args.length < 2) {
                System.out.println("Usage: Client host port");
            } else {
                LOG.info("Started " + Arrays.toString(args));
                try (final Client client = new Client(args[0], Integer.parseInt(args[1]))) {
                    basicTest(client);
                    concurrentTest(client);
                }
                ;
                LOG.info("Finished.");
            }
        } catch (Throwable e) {
            LOG.error("Caught exception at top level", e);
            System.err.println(e.getMessage());
            System.exit(0);
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
        final ForkJoinPool forkJoin = new ForkJoinPool(8);
        IntStream.range(0, 10).forEach(ti -> forkJoin.execute(new Caller(ti, client)));
        //noinspection StatementWithEmptyBody
        while (!forkJoin.awaitQuiescence(1, TimeUnit.MINUTES)) ;
    }
}
