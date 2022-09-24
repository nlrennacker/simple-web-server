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
     * @return
     */
    public byte[] getBody(){
        return body;
    }

    public void setBadRequest(){
        this.badRequest = true;
    }

    public boolean isInvalidRequest() {
        return badRequest;
    }

    public String getRequestLine() {
        return String.format(
                "%s %s %s",
                this.method,
                this.getID(),
                "HTTP/1.1"
        );
    }
}
