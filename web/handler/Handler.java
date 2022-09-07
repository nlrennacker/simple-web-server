package web.handler;

import web.request.HTTPRequest;
import web.response.HTTPResponse;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;

public class Handler implements Runnable {
    
    private ServerSocket serverSocket;
    private Socket socket;
    private OutputStream outputStream;
    private final int port;

    public Handler(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try {
           this.socketBind();
           this.listen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void socketBind() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Listening on port: " + port);
    }

    private void listen() throws IOException {
        while (true) {
            socket = serverSocket.accept();
            System.out.println("Connection Established: " + socket.getInetAddress());

            HTTPRequest request = new HTTPRequest(socket);

            HTTPResponse response = new HTTPResponse();
            response.addHeader("Connection", "close");

            if (request.getFullRequest().contains("sushi")) {
                File testImage = new File("public_html/images/sushi.jpg");
                response.addHeader("Content-Type", "image/jpeg");
                response.setBody(Files.readAllBytes(testImage.toPath()));
            } else {
                File testHtml = new File("public_html/index.html");
                response.addHeader("Content-Type", "text/html");
                response.setBody(Files.readAllBytes(testHtml.toPath()));
            }

            outputStream = socket.getOutputStream();
            response.writeResponse(outputStream);
            outputStream.flush();
            outputStream.close();
            socket.close();
        }
    }
}
