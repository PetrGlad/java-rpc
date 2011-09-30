package petrglad.javarpc.service;

public class ExceptionFactory {    
    public Object blow() {
        throw new RuntimeException("Never succeed");
    }
}
