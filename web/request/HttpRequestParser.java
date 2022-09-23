package web.request;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;

public class HttpRequestParser {

    private String fullRequest;
    private HTTPRequest request;

    //more readable regex
    private static HashMap<String, String> regex;
    static {
        regex = new HashMap<>();
        regex.put("carriageReturn", "\\R"); // java new line os independent
        regex.put("headerSplit", "(?s:.)+?(?<=Content-Length: \\d{0,100}\\R)"); // everything before (and including Content-Length: ... \r\n)
    }


    public HttpRequestParser(Socket socket, HTTPRequest request) throws IOException {
        try {
            fullRequest = readFullMessage(socket);
            this.request = request;
            parseInput();
        } catch (Exception e){
            System.err.println("Error occurred when creating request");
            e.printStackTrace();
        }
    }

    private void parseInput() throws IOException {
        // [0] for getting just the first line of the full request
        splitHeader(fullRequest.split(regex.get("carriageReturn"))[0]); 

        switch (request.getMethod()) {
            // request body is disregarded if there is one
            case "GET":
            case "HEAD":
                parseHeaders(fullRequest);
                break;

            // requests MUST have body but can be empty
            case "POST":
            case "PUT":
                if (fullRequest.contains("Content-Length: ")) {
                    // removing body (it's in [1])
                    String[] headerBodySplit = fullRequest.split(regex.get("headerSplit")); 
                    parseHeaders(headerBodySplit[0]);
                    if (headerBodySplit.length > 1) {
                        request.setBody(headerBodySplit[1].trim());
                    }
                } else {
                    // TODO
                    // CREATE FLAG FOR IMPROPER FORMAT
                }
                break;

            // requests MAY have body
            case "DELETE":
                if (fullRequest.contains("Content-Length: ")) {
                    // removing body (it's in [1])
                    String[] headerBodySplit = fullRequest.split(regex.get("headerSplit")); 
                    parseHeaders(headerBodySplit[0]);
                    if (headerBodySplit.length > 1) {
                        request.setBody(headerBodySplit[1].trim());
                    }
                } else {

                }
                break;

            default:
                // TODO
                // RAISE FLAG FOR UNSUPPORTED METHOD
                break;
        }
    }

    // ensures that entire http request is read in (because requests are parsed in
    // series of packets and those may not be loaded into memory instantly)
    private String readFullMessage(Socket socket) throws IOException {
        BufferedInputStream stream = new BufferedInputStream(socket.getInputStream());
        StringBuilder result = new StringBuilder();
        do {
            result.append((char) stream.read());
        } while (stream.available() > 0);
        return result.toString();
    }

    private void splitHeader(String fullHeader) {
        String[] splitHeader = fullHeader.split("\\s+");

        if (splitHeader.length >= 3) {
            this.request.setMethodAndIDs(splitHeader);
        }
    }

    private void parseHeaders(String headers) {
        String[] carriageSplit = headers.split(regex.get("carriageReturn"));

        for (String line : carriageSplit) {
            String identAndData[] = line.split(": ");
            String removeDash = identAndData[0].trim().toUpperCase().replace("-", "_");

            // ensures that header is valid and recognized header according to standards
            if (Header.contains(removeDash)) {
                request.setHeader(Header.valueOf(removeDash), identAndData[1]);
            } else {
                // throw flag for unrecognized header
                // DOES NOT HAVE TO STOP REQUEST
            }

        }
    }

    //TODO
    //REMOVE OR CHANGE THIS FUNCTIONALITY
    /**
     * Returns the full http Request for debug purposes
     * @return
     */
    public String getFullRequest() {
        return fullRequest;
    }
}