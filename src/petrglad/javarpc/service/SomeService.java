package petrglad.javarpc.service;

import java.util.Date;

/**
 * Service from task description.
 */
public class SomeService {
    public void sleep(Long millis) throws InterruptedException {
        Thread.sleep(millis.longValue());
    }

    public Date getCurrentDate() {
        return new Date();
    }
}
