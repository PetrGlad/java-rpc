package petrglad.javarpc;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Logger;

public final class Utils {
    
    static final Logger LOG = Logger.getLogger(Utils.class);
    
    private Utils() {
    }

    public static void closeSocket(final ServerSocket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            LOG.error("Error closing server socket " + socket, e);
        }
    }
    
    public static void closeSocket(final Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            LOG.error("Error closing socket " + socket, e);
        }
    }
}
