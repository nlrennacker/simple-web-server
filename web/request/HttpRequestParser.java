package web.request;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;

public class HttpRequestParser {

    private HttpRequest request;
    private String requestLine;
    private String headerString;
    private byte[] rawBody;


    public HttpRequestParser(Socket socket, HttpRequest request) {
        try {
            this.request = request;
            readAndParseFullMessage(socket);
        } catch (Exception e) {
            System.err.println("Error occurred when creating request");
            e.printStackTrace();
        }
    }

    // ensures that entire http request is read in (because requests are parsed in
    // series of packets and those may not be loaded into memory instantly)
    private void readAndParseFullMessage(Socket socket) throws IOException {
        BufferedInputStream stream = new BufferedInputStream(socket.getInputStream());

        // Read and parse headers from input stream
        StringBuilder requestMessageBuilder = new StringBuilder();
        byte[] previousBytes = new byte[3];
        do {
            int b = stream.read();
            if (b == -1) {
                break;
            }
            requestMessageBuilder.append((char) b);
            if (previousBytes[0] == '\r' && previousBytes[1] == '\n' && previousBytes[2] == '\r' && b == '\n') {
                break;
            }
            previousBytes[0] = previousBytes[1];
            previousBytes[1] = previousBytes[2];
            previousBytes[2] = (byte) b;
        } while (stream.available() > 0);

        // Set request fields (method, id, headers)
        String requestMessage = requestMessageBuilder.toString();
        int requestMessageFirstCrlf = requestMessage.indexOf("\r\n");
        if (requestMessageFirstCrlf == -1) {
            this.requestLine = requestMessage;
        } else {
            this.requestLine = requestMessage.substring(0, requestMessageFirstCrlf + 2);
            this.headerString = requestMessage.substring(requestMessageFirstCrlf + 2);
            parseHeaders(this.headerString);
        }
        setRequestMethodAndIDs(this.requestLine);

        // Read body from input stream
        if (this.request.hasHeader(Header.CONTENT_LENGTH)) {
            this.rawBody = new byte[Integer.parseInt(this.request.getHeaderValue(Header.CONTENT_LENGTH))];
            int i = 0;
            do {
                int b = stream.read();
                if (b == -1) {
                    break;
                }
                if (i >= this.rawBody.length) {
                    break;
                }
                this.rawBody[i++] = (byte) b;
            } while (stream.available() > 0);
        }

        // Set request body
        switch (this.request.getMethod()) {
            // request body is disregarded if there is one
            case "GET":
            case "HEAD":
                break;

            // requests MUST have body but can be empty
            case "POST":
            case "PUT":
                if (this.rawBody != null) {
                    request.setBody(this.rawBody);
                } else {
                    request.setBadRequest();
                }
                break;

            // requests MAY have body
            case "DELETE":
                if (this.rawBody != null) {
                    request.setBody(this.rawBody);
                }
                break;

            default:
                request.setBadRequest();
                break;
        }
    }

    private void setRequestMethodAndIDs(String requestLine) {
        String[] requestLineTokens = requestLine.split(" ");

        if (requestLineTokens.length != 3) {
            request.setBadRequest();
        } else {
            this.request.setMethodAndIDs(requestLineTokens);
        }
    }

    private void parseHeaders(String headers) {
        try {
            String[] carriageSplit = headers.split("\r\n");

            for (String line : carriageSplit) {
                String[] fieldAndValue = line.split(": ");
                String field = fieldAndValue[0];
                String value = fieldAndValue[1];
                String formattedField = field.trim().toUpperCase().replace("-", "_");

                // ensures that header is valid and recognized according to standards
                if (Header.contains(formattedField)) {
                    this.request.setHeader(Header.valueOf(formattedField), value);
                } else {
                    // throw flag for unrecognized header
                    // DOES NOT HAVE TO STOP REQUEST
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            this.request.setBadRequest();
        }
    }
}