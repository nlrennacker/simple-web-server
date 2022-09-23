package web.request;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpRequestParser {

    private String fullRequest;
    private HttpRequest request;

    // more readable regex
    private static final HashMap<String, String> regex;
    static {
        regex = new HashMap<>();
        regex.put("carriageReturn", "\\R"); // java new line os independent
        regex.put("headerSplit", "(?s:.)+?(?<=Content-Length: \\d{0,100}\\R)"); // everything before (and including
                                                                                // Content-Length: ... \r\n)
    }

    public HttpRequestParser(Socket socket, HttpRequest request) throws IOException {
        try {
            fullRequest = readFullMessage(socket);
            this.request = request;
            parseInput();
        } catch (Exception e) {
            System.err.println("Error occurred when creating request");
            e.printStackTrace();
        }
    }

    private void parseInput() throws IOException {
        // [0] for getting just the first line of the full request
        splitHeader(fullRequest.split(regex.get("carriageReturn"))[0]);
        Pattern p = Pattern.compile(regex.get("headerSplit"));
        Matcher m = p.matcher(fullRequest);
        
        switch (request.getMethod()) {
            // request body is disregarded if there is one
            case "GET":
            case "HEAD":
                parseHeaders(fullRequest);
                break;

            // requests MUST have body but can be empty
            case "POST":
            case "PUT":
                if (m.find()) {
                    String headers = m.group(0);
                    String body = fullRequest.substring(m.group(0).length()).trim();
                    parseHeaders(headers);
                    if (!body.equals("")) {
                        request.setBody(body);
                    }
                } else {
                    request.setBadRequest();
                }
                break;

            // requests MAY have body
            case "DELETE":
                if (m.find()) {
                    String headers = m.group(0);
                    String body = fullRequest.substring(m.group(0).length()).trim();
                    parseHeaders(headers);
                    if (!body.equals("")) {
                        request.setBody(body);
                    }
                }
                break;

            default:
                request.setBadRequest();
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

        if (splitHeader.length < 3) {
            request.setBadRequest();
        } else {
            this.request.setMethodAndIDs(splitHeader);
        }
    }

    private void parseHeaders(String headers) {
        String[] carriageSplit = headers.split(regex.get("carriageReturn"));

        for (String line : carriageSplit) {
            String[] identAndData = line.split(": ");
            String removeDash = identAndData[0].trim().toUpperCase().replace("-", "");

            // ensures that header is valid and recognized according to standards
            if (Header.contains(removeDash)) {
                request.setHeader(Header.valueOf(removeDash), identAndData[1]);
            } else {
                // throw flag for unrecognized header
                // DOES NOT HAVE TO STOP REQUEST
            }

        }
    }

    // TODO
    // REMOVE OR CHANGE THIS FUNCTIONALITY
    /**
     * Returns the full http Request for debug purposes
     * 
     * @return
     */
    public String getFullRequest() {
        return fullRequest;
    }
}