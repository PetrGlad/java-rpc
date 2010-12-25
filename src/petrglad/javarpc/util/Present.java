package petrglad.javarpc.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Present<T> implements Future<T> {
	final T value;
	
	public Present(T value) {
		this.value = value;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return true;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return true;
	}

	@Override
	public T get() throws InterruptedException,
			ExecutionException {
		return value;
	}

	@Override
	public T get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException,
			TimeoutException {
		return value;
	}
}