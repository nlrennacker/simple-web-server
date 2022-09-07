package web.request;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class HTTPRequest {

    private String method;
    private String identifier;
    private String version;
    private InetAddress INet;

    public HTTPRequest(Socket socket) throws IOException {
        this.INet = socket.getInetAddress();
        HttpRequestParser tempParser = new HttpRequestParser(socket, this);
        System.out.println("Request Recieved: \n" + tempParser.getFullRequest());
    }

    public void setHeader(String[] headers){
        this.method = headers[0];
        this.identifier = headers[1];
        this.version = headers[2];
    }

    public String getMethod() {
        return method;
    }

}