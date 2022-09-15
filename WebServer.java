import web.handler.Handler;
import web.server.configuration.HttpdConf;
import web.server.configuration.MimeTypes;
import web.server.configuration.utils.ConfigurationReader;

import java.io.IOException;
import java.net.ServerSocket;

public class WebServer {
    private static final Integer DEFAULT_PORT = 8080;

    private static HttpdConf httpdConf;
    private static MimeTypes mimeTypes;

    public static void main(String[] args) throws IOException {
        loadConfigs();
        startServer();
    }
    private static void loadConfigs() throws IOException {
        httpdConf = new HttpdConf(ConfigurationReader.readConfiguration("conf/httpd.conf"));
        mimeTypes = new MimeTypes(ConfigurationReader.readConfiguration("conf/mime.types"));
    }

    private static void startServer() {
        Integer port = httpdConf.getListen().orElse(DEFAULT_PORT);
        // Create socket using try-with-resources
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Listening on port: " + port);

            while (true) {
                Handler handler = new Handler(serverSocket.accept(), httpdConf, mimeTypes);
                Thread thread = new Thread(handler);
                thread.start();
            }
        } catch (IOException e) {
            System.err.printf("Failed to open server on port (%d), exiting...%n", port);
            e.printStackTrace();
        }
    }
}
