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
    private String body = "";
    private String fullRequest = "";

    public HTTPRequest(Socket socket) throws IOException{
        //this.INet = socket.getInetAddress();
        HttpRequestParser parser = new HttpRequestParser(socket, this);
        System.out.println("Request Received: \n" + parser.getFullRequest());
    }

    /**
     * Sets the 3 initial required parts of the http request
     * Method
     * Identifier
     * Version
     * @param array contains those three parts each their own element
     */
    public void setMethodAndIDs(String[] array){
        this.method = array[0];
        this.identifier = array[1];
        this.version = array[2];
    }

    /**
     * Returns the method of the Http Request
     * @return String method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Returns the ID of the Http Request
     * @return String Identifier
     */
    public String getID(){
        return identifier;
    }

    /**
     * Sets the specified Header (enum value) with its corresponding value in a Hashmap<Header, String>
     * @param header enum
     * @param value header value
     */
    public void setHeader(Header header, String value){
        headers.put(header, value);
    }

    /**
     * Returns the header value matching with the specified Header key (enum)
     * @param header enum
     * @return matching value stored within hashmap
     */
    public String getHeaderValue(Header header){
        return headers.get(header);
    }
    
    /**
     * Sets the http request body
     * @param body string
     */
    public void setBody(String body){
        this.body = body;
    }
    
    /**
     * Returns the http request body if there is one, otherwise will return blank value
     * @return
     */
    public String getBody(){
        return body;
    }
    
    //TODO
    //REMOVE THIS FUNCTION EVENTUALLY
    /**
     * Returns the full http request parsed from HTTP Request Parser
     * @return full http request string
     */
    public String getFullRequest() {
        return fullRequest;
    }
}