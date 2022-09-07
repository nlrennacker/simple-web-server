package web.request;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class HTTPRequest {

    private String method;
    private String identifier;
    private String version;
    private InetAddress INet;
    private String fullRequest;

    public HTTPRequest(Socket socket) throws IOException {
        this.INet = socket.getInetAddress();
        HttpRequestParser tempParser = new HttpRequestParser(socket, this);
        this.fullRequest = tempParser.getFullRequest();
        System.out.println("Request Received: \n" + this.fullRequest);
    }

    public void setHeader(String[] headers){
        this.method = headers[0];
        this.identifier = headers[1];
        this.version = headers[2];
    }

    public String getMethod() {
        return method;
    }

    public String getFullRequest() {
        return this.fullRequest;
    }
}