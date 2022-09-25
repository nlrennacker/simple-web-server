package web.request;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class HttpRequest {

    private String method = "NONE";
    private String identifier;
    private String version;
    private final HashMap<Header,String> headers = new HashMap<>();
    private byte[] body;
    private boolean badRequest = false;

    /**
     * Creates an populates http request fields for object including Method, Identifier, Version, Headers(hashMap), and an optional body
     * @param socket any Socket with a httpRequest in the inputStream
     * @throws IOException if socket is lost somewhere along the way
     */
    public HttpRequest(Socket socket) throws IOException {
        new HttpRequestParser(socket, this);
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

    public String getID() {
        String identifier = Optional.ofNullable(this.identifier).orElse("");
        int indexOfInterrobang = identifier.indexOf('?');
        if (indexOfInterrobang == -1) {
            return identifier;
        } else {
            return identifier.substring(0, indexOfInterrobang);
        }
    }

    /**
     * Returns the ID of the Http Request
     * @return String Identifier
     */
    public Optional<String> getQueryString() {
        String identifier = Optional.ofNullable(this.identifier).orElse("");
        int indexOfInterrobang = identifier.indexOf('?');
        if (indexOfInterrobang == -1) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(identifier.substring(indexOfInterrobang + 1));
        }
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
     * If the request has a specific header TRUE is returned, otherwise FALSE is returned
     * @param header to search for
     * @return TRUE if header is found, otherwise FALSE
     */
    public boolean hasHeader(Header header){
        return headers.containsKey(header);
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
     * 
     * @return the full map of request headers
     */
    public Map<Header,String> getHeaders(){
        return headers;
    }


    /**
     * Sets the http request body
     * @param body string
     */
    public void setBody(byte[] body){
        this.body = body;
    }
    
    /**
     * Returns the http request body if there is one, otherwise will return blank value
     * @return request body in Byte[] format can be blank
     */
    public byte[] getBody(){
        return body;
    }
    
    /**
     * if the request fails to parse -> bad request should be flagged
     */
    public void setBadRequest(){
        this.badRequest = true;
    }

    /**
     * Returns TRUE if the badRequest boolean is ever flagged
     * @return TRUE if badRequest is ever flagged as true.
     */
    public boolean isInvalidRequest() {
        return badRequest;
    }

    /**
     * Formats the request line exactly how it was recieved but surrounded by quotes
     * @return formatted request line surrounded by quotes -> "Method URI httpVersion"
     */
    public String getRequestLine() {
        return String.format(
                "%s %s %s",
                this.method,
                this.getID(),
                this.version
        );
    }
}
