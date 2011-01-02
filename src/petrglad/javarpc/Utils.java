package petrglad.javarpc;

import java.io.IOException;
import java.net.Socket;

public final class Utils {
    private Utils() {
    }

    public static void closeSocket(final Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
        }
    }
}
