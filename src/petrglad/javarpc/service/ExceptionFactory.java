package petrglad.javarpc.service;

import org.apache.log4j.Logger;

public class ExceptionFactory {
    static final private Logger LOG = Logger.getLogger(ExceptionFactory.class);
    
    public void blow() {
        throw new RuntimeException("Never succeed");
    }
    
    public void voidMethod() {
        LOG.debug("Void method is called.");
    }
}
