package web.response;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for creating HTTP responses.
 * Defaults to a 200 OK status.
 * Automatically sets headers for:
 * - Server
 * - Date
 * - Content-Length
 */
public class HTTPResponse {
    private static final Map<Integer, String> REASON_PHRASES = Map.of(
            200, "OK",
            201, "Created",
            204, "No Content",
            304, "Not Modified",
            400, "Bad Request",
            401, "Unauthorized",
            403, "Forbidden",
            404, "Not Found",
            500, "Internal Server Error"
    );

    private String httpVersion;
    private Integer statusCode;
    private Map<String, String> headers;
    private byte[] body;

    public HTTPResponse() {
        this.httpVersion = "HTTP/1.1";
        this.statusCode = 200;
        this.headers = new HashMap<>();
    }

    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public void addHeader(String key, String value) {
        this.headers.put(key, value);
    }

    public void addHeaders(Map<String, String> headers) {
        this.headers.putAll(headers);
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void setBody(byte[] data) {
        this.body = data;
    }

    public void writeResponse(OutputStream outputStream) throws IOException {
        // Set mandatory headers specified in project spec
        this.headers.put("Server", "Chan Rennacker");
        this.headers.put("Date", (new Date()).toString());

        // Automatically add other headers
        if (this.body != null) {
            this.headers.put("Content-Length", String.valueOf(this.body.length));
        }

        outputStream.write(String.format("%s\r\n", getStatusLine()).getBytes(StandardCharsets.ISO_8859_1));
        for (Map.Entry<String, String> header : this.headers.entrySet()) {
            outputStream.write(String.format("%s: %s\r\n", header.getKey(), header.getValue()).getBytes(StandardCharsets.ISO_8859_1));
        }
        outputStream.write("\r\n".getBytes(StandardCharsets.ISO_8859_1)); // There must be CRLF after the status line and headers.
        if (this.body != null) {
            outputStream.write(this.body);
        }
    }

    private String getStatusLine() {
        String reasonPhrase = HTTPResponse.REASON_PHRASES.get(this.statusCode);

        return String.format(
                "%s %d %s",
                this.httpVersion,
                this.statusCode,
                reasonPhrase);
    }
}
