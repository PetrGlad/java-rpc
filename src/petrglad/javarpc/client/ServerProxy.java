package petrglad.javarpc.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import petrglad.javarpc.Message;
import petrglad.javarpc.Response;

/**
 * Handles connection to server and message serialization.
 * 
 * @author petr
 */
public class ServerProxy {

    private final String host;
    private final int port;

    private Socket socket = null;
    private ObjectOutputStream oOut = null;
    private ObjectInputStream oIn = null;

    public ServerProxy(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void open() {
        assert null == socket;
        assert null == oOut;
        assert null == oIn;
        try {
            socket = new Socket(host, port);
            oOut = new ObjectOutputStream(socket.getOutputStream());
            oIn = new ObjectInputStream(socket.getInputStream());
        } catch (UnknownHostException e) {
            throw new RuntimeException("Host is not found " + host + ":" + port);
        } catch (IOException e) {
            throw new RuntimeException("Could not open connection to " + host);
        }
    }

    public void send(Message m) {
        try {
            synchronized (oOut) {
                oOut.writeObject(m);
                oOut.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Response receive() {
        try {
            synchronized (oIn) {
                final Object o = oIn.readObject();
                if (o instanceof Response) {
                    return (Response) o;
                } else {
                    throw new RuntimeException(
                            "Response object is of unexpected type "
                                    + o.getClass().getCanonicalName());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            oOut.close();
        } catch (IOException e) {
        }
        try {
            oIn.close();
        } catch (IOException e) {
        }
        try {
            socket.close();
        } catch (IOException e) {
        }
    }
}
