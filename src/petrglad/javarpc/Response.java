package petrglad.javarpc;

import java.io.Serializable;

public class Response implements Serializable {
	private static final long serialVersionUID = 8271885632220176998L;

	public final long serialId;
	public final Object value;

	private Response(long serialId, Object value) {
		this.serialId = serialId;
		this.value = value;
	}

	public Response(Message msg, Object e) {
		this(msg.serialId, e);
	}

	public RuntimeException asException() {
		// Assuming that exceptions are not returned during normal operation.
		if (value instanceof RuntimeException)
			return (RuntimeException) value;
		else if (value instanceof Throwable)
			return new RuntimeException((Throwable)value);
		else
			return null;
	}
}
