package petrglad.javarpc.util;

import org.apache.log4j.Logger;

import com.google.common.base.Supplier;

/**
 * Runnable that sequentially processes messages from input queue with given processor.
 * 
 * TODO (refactoring) Use service classes from guava here?
 * 
 * @author petr
 */
public class Spooler<T> implements Runnable {
    static final Logger LOG = Logger.getLogger(Spooler.class);
    final Flag isRunning;
    final Supplier<T> source;
    // XXX Need more appropriate interface here
    final Sink<T> process;

    public Spooler(final Supplier<T> source, final Sink<T> process,
            Flag isRunning) {
        this.source = source;
        this.process = process;
        this.isRunning = isRunning;
    }

    @Override
    public void run() {
        while (isRunning.get()) {
            T v = source.get();
            if (null == v)
                return; // EOF
            else
                process.put(v);
        }
    }
}
