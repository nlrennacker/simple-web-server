package web.handler;

import java.net.ServerSocket;
import java.net.Socket;

import web.request.HTTPRequest;
import web.response.HTTPResponse;

import java.io.IOException;
import java.io.PrintWriter;

public class Handler implements Runnable{
    
    private ServerSocket serverSocket;
    private Socket socket;
    private PrintWriter outStream;
    private int port;

    public Handler(int port){
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

    private void socketBind() throws IOException{
        serverSocket = new ServerSocket(port);
        System.out.println("Listening on port:" + port);
    }

    private void listen() throws IOException{
        while(true){
                socket = serverSocket.accept();
                System.out.println("Connection Established: " + socket.getInetAddress());

                HTTPRequest request = new HTTPRequest(socket);
                //HTTPResponse response =  new HTTPResponse();
                outStream = new PrintWriter(socket.getOutputStream());

                outStream.print("HTTP/1.1 200 \r\n");
                outStream.print("Content-Type: text/html\r\n");
                outStream.print("Connection: close\r\n");
                outStream.print("\r\n");
                outStream.print("<!doctype html>\n");
                outStream.print("<title>Test title</title>\n");
                outStream.print("<p>Test</p>\n");
                outStream.flush();
                outStream.close();
                socket.close();
            }
    }
    
}
