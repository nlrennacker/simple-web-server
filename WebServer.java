import web.handler.Handler;
import web.resource.ConfigResource;

import java.io.IOException;
import java.net.ServerSocket;

public class WebServer {
    private static final Integer DEFAULT_PORT = 8080;
    public static void main(String[] args) throws IOException {
        startServer();
    }
    private static void startServer() {
        Integer port = ConfigResource.getHttpdConf().getListen().orElse(DEFAULT_PORT);
        // Create socket using try-with-resources
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Listening on port: " + port);

            while (true) {
                Handler handler = new Handler(serverSocket.accept());
                Thread thread = new Thread(handler);
                thread.start();
            }
        } catch (IOException e) {
            System.err.printf("Failed to open server on port (%d), exiting...%n", port);
            e.printStackTrace();
        }
    }
}
