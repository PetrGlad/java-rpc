package petrglad.javarpc.util;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public final class Sockets {
    private Sockets() {    
    }    
    
    public static Socket openClientSocket(final String host, final int port) {
        try {
            return new Socket(host, port);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Host is not found " + host + ":" + port);
        } catch (IOException e) {
            throw new RuntimeException("Could not open connection to " + host);
        }
    }

    /**
     * @return Stop condition for spoolers.
     */
    public static Flag getIsSocketOpen(final Socket socket) {
        return new Flag() {
            @Override
            public Boolean get() {
                return !socket.isClosed();
            }
        };
    }
}
