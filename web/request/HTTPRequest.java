package web.request;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;

public class HTTPRequest {

    private InetAddress INet;
    private String method = "none";
    private String identifier;
    private String version;
    private HashMap<Header,String> headers = new HashMap<>();
    private String body;
    private String fullRequest = "";

    public HTTPRequest(Socket socket) throws IOException{
        //this.INet = socket.getInetAddress();
        HttpRequestParser parser = new HttpRequestParser(socket, this);
        System.out.println("Request Received: \n" + parser.getFullRequest());
    }

    public void setMethodAndIDs(String[] headers){
        this.method = headers[0];
        this.identifier = headers[1];
        this.version = headers[2];
    }

    public String getMethod() {
        return method;
    }

    public String getID(){
        return identifier;
    }

    public void setHeader(Header header, String value){
        headers.put(header, value);
    }

    public String getHeaderValue(Header header){
        return headers.get(header);
    }
    
    public void setBody(String body){
        this.body = body;
    }
    
    public String getBody(){
        return body;
    }

    public String getFullRequest() {
        return fullRequest;
    }
}