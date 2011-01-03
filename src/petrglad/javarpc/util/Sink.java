package petrglad.javarpc.util;

public interface Sink<T> {
    void put(T v);
}
