package petrglad.javarpc.service;

import java.util.Random;

public class Calculator {

    final Random random = new Random();

    public Long add(Long a, Long b) {
        return a + b;
    }

    /**
     * Example of some "long running" operation.
     */
    public Long guess(Long a, Long b) {
        try {
            Thread.sleep(0, random.nextInt(9999));
        } catch (InterruptedException e) {
        }
        return a * b;
    }
}
