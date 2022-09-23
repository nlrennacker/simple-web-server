package web.handler;

import web.request.HTTPRequest;
import web.request.Header;
import web.response.HTTPResponse;
import web.server.configuration.HttpdConf;
import web.server.configuration.MimeTypes;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

public class Handler implements Runnable {

    private static final String DEFAULT_DIRECTORY_INDEX = "index.html";
    private static final String DEFAULT_MIME_TYPE = "text/text";
    private static final String HT_ACCESS_FILENAME = ".htaccess";

    private final Socket socket;
    private final HttpdConf httpdConf;
    private final MimeTypes mimeTypes;
    private int bytesSent;
    private HTTPRequest request;
    private HTTPResponse response;
    private AuthorizationChecker authorizationChecker;

    public Handler(Socket socket, HttpdConf httpdConf, MimeTypes mimeTypes) throws IOException {
        this.socket = socket;
        this.httpdConf = httpdConf;
        this.mimeTypes = mimeTypes;
        this.request = new HTTPRequest(socket);
        this.response = new HTTPResponse();
    }

    @Override
    public void run() {
        try {
            this.response.addHeader("Connection", "close");

            if (!this.request.isValidRequest()) {
                this.response.setStatusCode(400);
                writeResponse();
                return;
            }

            // Resolve request URI to an absolute path
            String requestUri = this.request.getID();
            boolean isScriptAliased = false;
            if (this.httpdConf.getScriptAliases().isPresent()) {
                for (Map.Entry<String, String> scriptAlias : this.httpdConf.getScriptAliases().get().entrySet()) {
                    if (requestUri.contains(scriptAlias.getKey())) {
                        isScriptAliased = true;
                        requestUri = requestUri.replace(scriptAlias.getKey(), scriptAlias.getValue());
                        break;
                    }
                }
            }
            Path requestPath;
            if (!isScriptAliased) {
                requestPath = Paths.get(this.httpdConf.getDocumentRoot().orElseThrow(), requestUri);
            } else {
                requestPath = Paths.get(requestUri);
            }
            if (Files.isDirectory(requestPath)) {
                requestPath = Paths.get(requestPath.toString(), this.httpdConf.getDirectoryIndex().orElse(DEFAULT_DIRECTORY_INDEX));
            }

            Path htAccessPath = Paths.get(requestPath.toAbsolutePath().getParent().toString(), HT_ACCESS_FILENAME);
            this.authorizationChecker = new AuthorizationChecker(htAccessPath);
            AuthorizationChecker.AuthorizationResult authorizationResult = this.authorizationChecker.checkAuthorization(request);
            if (authorizationResult.equals(AuthorizationChecker.AuthorizationResult.MISSING_AUTH)) {
                this.response.setStatusCode(401);
                this.response.addHeader("WWW-Authenticate", this.authorizationChecker.getWWWAuthenticateHeader());
                writeResponse();
                return;
            }
            if (authorizationResult.equals(AuthorizationChecker.AuthorizationResult.INVALID)) {
                this.response.setStatusCode(403);
                writeResponse();
                return;
            }

            if (Files.notExists(requestPath)) {
                this.response.setStatusCode(404);
                writeResponse();
                return;
            }

            if (isScriptAliased) {
                ProcessBuilder processBuilder = new ProcessBuilder(requestPath.toString());

                Map<String, String> env = processBuilder.environment();
                env.put("SERVER_PROTOCOL", "HTTP/1.1");
                this.request.getQueryString().ifPresent((queryString) -> env.put("QUERY_STRING", queryString));
                Map<Header, String> requestHeaders = this.request.getHeaders();
                for (Map.Entry<Header, String> requestHeader : requestHeaders.entrySet()) {
                    env.put("HTTP_".concat(requestHeader.getKey().toString()), requestHeader.getValue());
                }

                byte[] body = this.request.getBody().getBytes();
                try {
                    Process process = processBuilder.start();

                    OutputStream outputStream = process.getOutputStream();
                    outputStream.write(body, 0, body.length);
                    outputStream.flush();
                    outputStream.close();
                    this.response.setStatusCode(200);
                    writeCgiResponse(process);
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    this.response.setStatusCode(500);
                    writeResponse();
                    return;
                }
            }

            switch (request.getMethod().toUpperCase()) {
                case "GET" -> {
                    String mimeType = this.mimeTypes.getMimeTypeForExtension(getFileExtension(requestPath)).orElse(DEFAULT_MIME_TYPE);
                    this.response.addHeader("Content-Type", mimeType);
                    this.response.setBody(Files.readAllBytes(requestPath));
                    writeResponse();
                    return;
                }
                default -> {
                    this.response.setStatusCode(200);
                    writeResponse();
                    return;
                }
            }
        } catch (Exception e) {
            System.out.println("Error while handling request:");
            e.printStackTrace();

            try {
                this.response.setStatusCode(500);
                writeResponse();
            } catch (IOException ignored) {}
        }
    }

    private void writeResponse() throws IOException {
        try (CountingOutputStream outputStream = new CountingOutputStream(this.socket.getOutputStream())) {
            this.response.writeResponse(outputStream);
            outputStream.flush();
            this.bytesSent = outputStream.getCount();
            logRequest();
        } catch (SocketException e) {
            // Handle the case where client closed the connection while server was writing to it
            this.socket.close();
        }
    }

    private void writeCgiResponse(Process process) throws IOException {
        try (CountingOutputStream outputStream = new CountingOutputStream(this.socket.getOutputStream())) {
            this.response.writeMinimalCgiResponse(outputStream);
            process.getInputStream().transferTo(outputStream);
            outputStream.flush();
            this.bytesSent = outputStream.getCount();
            logRequest();
        } catch (SocketException e) {
            // Handle the case where client closed the connection while server was writing to it
            this.socket.close();
        }
    }

    private String getFileExtension(Path path) {
        String filename = path.toString();
        return Optional.of(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1))
                .orElse("");
    }

    private void logRequest() throws IOException {
        String host = this.socket.getInetAddress().getHostAddress();
        String ident = "-";
        String authuser = this.authorizationChecker != null ? this.authorizationChecker.getCheckedUser().orElse("-") : "-";
        String date = new SimpleDateFormat("d/MMM/yyyy:hh:mm:ss Z").format(new Date());
        String request = this.request.getRequestLine();
        String status = String.valueOf(this.response.getStatusCode());
        String bytes = String.valueOf(this.bytesSent);
        String logString = String.format(
            "%s %s %s [%s] \"%s\" %s %s\n",
            host,
            ident,
            authuser,
            date,
            request,
            status,
            bytes
        );

        // Log to stdout
        System.out.print(logString);

        // Log to file
        File file = new File(this.httpdConf.getLogFile().orElse("log.txt"));
        if (file.isDirectory()) {
            System.out.printf("Error: Cannot log to file, configured log file %s is a directory!\n", file.getAbsolutePath());
            return;
        }
        // Create directories if needed
        File parentDirectory = new File(file.getParent());
        if (!parentDirectory.exists()) {
            parentDirectory.mkdirs();
        }
        // Append log string, creating logfile if needed
        Files.writeString(
            Paths.get(file.toURI()),
            logString,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        );
    }
}
