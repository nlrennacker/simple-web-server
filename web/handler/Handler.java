package web.handler;

import web.authorization.AuthorizationChecker;
import web.request.HttpRequest;
import web.request.Header;
import web.resource.ConfigResource;
import web.resource.HttpResource;
import web.response.HttpResponse;

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

    private static final String DEFAULT_MIME_TYPE = "text/text";
    private static final String HT_ACCESS_FILENAME = ".htaccess";

    private final Socket socket;
    private final HttpRequest request;
    private final HttpResponse response;
    private AuthorizationChecker authorizationChecker;
    private int bytesSent;

    public Handler(Socket socket) throws IOException {
        this.socket = socket;
        this.request = new HttpRequest(socket);
        this.response = new HttpResponse();
    }

    /**
     * Accepts http requests and generates http responses...
     * <p>
     * Accepted http methods are HEAD, GET, PUT, POST, DELETE
     * <p/>
     * Overridden run function (from runnable interface) for threading support
     */
    @Override
    public void run() {
        try {
            this.response.addHeader("Connection", "close");

            if (this.request.isInvalidRequest()) {
                this.response.setStatusCode(400);
                this.writeResponse();
                return;
            }

            HttpResource resource = new HttpResource(request);

            // side note for debugging, http auth is stored per browser, so if viewing auth
            // in browser window (as opposed to postman) you will not get the auth popup
            // unless you close and reopen the browser each time
            Path htAccessPath = Paths.get(resource.getPath().toAbsolutePath().getParent().toString(), HT_ACCESS_FILENAME);
            this.authorizationChecker = new AuthorizationChecker(htAccessPath);
            AuthorizationChecker.AuthorizationResult authorizationResult = this.authorizationChecker.checkAuthorization(this.request);
            if (authorizationResult.equals(AuthorizationChecker.AuthorizationResult.MISSING_AUTH)) {
                this.response.setStatusCode(401);
                this.response.addHeader("WWW-Authenticate", this.authorizationChecker.getWWWAuthenticateHeader());
                this.writeResponse();
                return;
            }
            if (authorizationResult.equals(AuthorizationChecker.AuthorizationResult.INVALID)) {
                this.response.setStatusCode(403);
                this.writeResponse();
                return;
            }

            if (Files.notExists(resource.getPath()) && !this.request.getMethod().equals("PUT")) {
                this.response.setStatusCode(404);
                this.writeResponse();
                return;
            }

            if (resource.getIsScriptAliased()) {
                this.handleCgi(resource);
            } else {
                this.focusResponse(this.request, this.response, resource);
            }
        } catch (Exception e) {
            System.out.println("Error while handling request:");
            e.printStackTrace();
            this.response.setStatusCode(500);
            this.writeResponse();
        }
    }

    private void focusResponse(HttpRequest request, HttpResponse response, HttpResource resource) throws IOException {
        switch (request.getMethod().toUpperCase()) {
            //difference between get and head
            //head produces get response but WITHOUT body (while still calculating the length of the body)
            case "GET","HEAD" -> {
                if (request.hasHeader(Header.IF_MODIFIED_SINCE) && resource.compareDateTime(request.getHeaderValue(Header.IF_MODIFIED_SINCE))) {
                    this.response.setStatusCode(304);
                    this.response.addHeader("Last-Modified", resource.getFileDateTimeToString());
                    this.writeResponse();
                    return;
                }
                String mimeType = ConfigResource.getMimeTypes().getMimeTypeForExtension(getFileExtension(resource.getPath())).orElse(DEFAULT_MIME_TYPE);
                this.response.addHeader("Content-Type", mimeType);
                this.response.setBody(Files.readAllBytes(resource.getPath()));
                if(request.getMethod().equalsIgnoreCase("HEAD")){
                    this.response.setSendBody();
                }
                this.writeResponse();
            }
            
            //creates or replaces file at supplied location
            case "PUT" -> {
                File file = new File(resource.getPath().toString());
                // Create directories if needed
                File parentDirectory = new File(file.getParent());
                if (!parentDirectory.exists()) {
                    parentDirectory.mkdirs();
                }
                if (file.createNewFile()) {
                    this.response.setStatusCode(201);
                } else {
                    this.response.setStatusCode(200);
                }
                Files.write(resource.getPath(), request.getBody());
                this.response.addHeader("Content-Location", request.getID());
                this.response.addHeader("Content-Type", "text/html");
                this.response.setBody(this.responseConcat(request).getBytes());
                this.writeResponse();
            }
            case "POST" -> {
                String mimeType = ConfigResource.getMimeTypes().getMimeTypeForExtension(getFileExtension(resource.getPath())).orElse(DEFAULT_MIME_TYPE);
                this.response.addHeader("Content-Type", mimeType);
                this.response.setBody(Files.readAllBytes(resource.getPath()));
                this.writeResponse();
            }
            case "DELETE" -> {
                if(Files.exists(resource.getPath())){
                    File file = new File(resource.getPath().toString());
                    if(file.delete()){
                        System.out.println("Successfully deleted: " + file.getName());
                        this.response.setStatusCode(204);
                    } else{
                        System.out.println("Failed to delete file: " + file.getName());
                        this.response.setStatusCode(404);
                    }
                    this.writeResponse();
                }

            }
            default -> {
                this.response.setStatusCode(200);
            }
        }

    }

    private void handleCgi(HttpResource resource) {
        ProcessBuilder processBuilder = new ProcessBuilder(resource.getPath().toString());

        Map<String, String> env = processBuilder.environment();
        env.put("SERVER_PROTOCOL", "HTTP/1.1");
        this.request.getQueryString().ifPresent((queryString) -> env.put("QUERY_STRING", queryString));
        Map<Header, String> requestHeaders = this.request.getHeaders();
        for (Map.Entry<Header, String> requestHeader : requestHeaders.entrySet()) {
            env.put("HTTP_".concat(requestHeader.getKey().toString()), requestHeader.getValue());
        }

        byte[] body = this.request.getBody();
        try {
            Process process = processBuilder.start();

            OutputStream outputStream = process.getOutputStream();
            if (body != null) {
                outputStream.write(body, 0, body.length);
            }
            outputStream.flush();
            outputStream.close();
            this.response.setStatusCode(200);
            this.writeCgiResponse(process);
            return;
        } catch (IOException e) {
            e.printStackTrace();
            this.response.setStatusCode(500);
            this.writeResponse();
            return;
        }
    }

    private String responseConcat(HttpRequest request) {
        String first = "Your content has been saved! \n Click <A href=\"";
        String second = "\">here</A> to view it.";
        return first + request.getID() + second;
    }

    private void writeResponse() {
        try (CountingOutputStream outputStream = new CountingOutputStream(this.socket.getOutputStream())) {
            this.response.writeResponse(outputStream);
            outputStream.flush();
            this.bytesSent = outputStream.getCount();
            this.logRequest();
        } catch (IOException e) {
            // Handle the case where client closed the connection while server was writing to it
            try {
                this.socket.close();
            } catch (IOException ex) {
                System.out.println("Error occurred when closing socket");
                ex.printStackTrace();
            }
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
            try {
                this.socket.close();
            } catch (IOException ex) {
                System.out.println("Error occurred when closing socket");
                ex.printStackTrace();
            }
        }
    }

    private String getFileExtension(Path path) {
        String filename = path.toString();
        return Optional.of(filename).filter(f -> f.contains(".")).map(f -> f.substring(filename.lastIndexOf(".") + 1)).orElse("");
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
        File file = new File(ConfigResource.getHttpdConf().getLogFile().orElse("log.txt"));
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

