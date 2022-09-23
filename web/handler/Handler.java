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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class Handler implements Runnable {

    private static final String DEFAULT_MIME_TYPE = "text/text";
    private static final String HT_ACCESS_FILENAME = ".htaccess";

    private final Socket socket;
    private final HttpRequest request;
    private final HttpResponse response;

    public Handler(Socket socket) throws IOException {
        this.socket = socket;
        this.request = new HttpRequest(socket);
        this.response = new HttpResponse();
    }

    @Override
    public void run() {
        try {
            System.out.println("Connection Established: " + socket.getLocalSocketAddress());

            response.addHeader("Connection", "close");

            if (request.isInvalidRequest()) {
                response.setStatusCode(400);
                writeResponse();
                return;
            }

            HttpResource resource = new HttpResource(request);

            // side note for debugging, http auth is stored per browser, so if viewing auth
            // in browser window (as opposed to postman) you will not get the auth popup
            // unless you close and reopen the browser each time
            Path htAccessPath = Paths.get(resource.getPath().toAbsolutePath().getParent().toString(), HT_ACCESS_FILENAME);
            AuthorizationChecker authorizationChecker = new AuthorizationChecker(htAccessPath);
            AuthorizationChecker.AuthorizationResult authorizationResult = authorizationChecker.checkAuthorization(request);
            if (authorizationResult.equals(AuthorizationChecker.AuthorizationResult.MISSING_AUTH)) {
                response.setStatusCode(401);
                response.addHeader("WWW-Authenticate", authorizationChecker.getWWWAuthenticateHeader());
                writeResponse();
                return;
            }
            if (authorizationResult.equals(AuthorizationChecker.AuthorizationResult.INVALID)) {
                response.setStatusCode(403);
                writeResponse();
                return;
            }

            if (Files.notExists(resource.getPath()) && !request.getMethod().equals("PUT")) {
                response.setStatusCode(404);
                writeResponse();
                return;
            }

            if (resource.getIsScriptAliased()) {
                handleCgi(resource);
            } else {
                focusResponse(request, response, resource);
            }
        } catch (Exception e) {
            System.out.println("Error while handling request:");
            e.printStackTrace();
            response.setStatusCode(500);
            this.writeResponse();
        }
    }

    private void focusResponse(HttpRequest request, HttpResponse response, HttpResource resource) throws IOException {

        switch (request.getMethod().toUpperCase()) {

            //difference between get and head
            //head produces get response but WITHOUT body (while still calculating the length of the body)
            case "GET","HEAD" -> {
                if (request.hasHeader(Header.IFMODIFIEDSINCE) && resource.compareDateTime(request.getHeaderValue(Header.IFMODIFIEDSINCE))) {
                    response.setStatusCode(304);
                    response.addHeader("Last-Modified:", resource.getFileDateTimeToString());
                    writeResponse();
                    return;
                }
                String mimeType = ConfigResource.getMimeTypes().getMimeTypeForExtension(getFileExtension(resource.getPath())).orElse(DEFAULT_MIME_TYPE);
                response.addHeader("Content-Type", mimeType);
                response.setBody(Files.readAllBytes(resource.getPath()));
                if(request.getMethod().equalsIgnoreCase("HEAD")){
                    response.setSendBody();
                }
                writeResponse();
            }
            //creates or replaces file at supplied location
            case "PUT" -> {
                //TODO Potentially write in protections for existing files that should not be overwritten
                File file = new File(resource.getPath().toString());
                if (file.createNewFile()) {
                    response.setStatusCode(201);
                } else {
                    response.setStatusCode(200);
                }
                Files.write(resource.getPath(), Collections.singletonList(request.getBody()), StandardCharsets.ISO_8859_1, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                response.addHeader("Content-Location", request.getID());
                response.addHeader("Content-Type", "text/html");
                response.setBody(this.responseConcat(request).getBytes());
                writeResponse();
            }
            case "POST" -> {

            }
            case "DELETE" -> {
                //TODO Potentially write in protections for existing files that should not be deleted
                if(Files.exists(resource.getPath())){
                    File file = new File(resource.getPath().toString());
                    if(file.delete()){
                        System.out.println("Successfully deleted: " + file.getName());
                    } else{
                        System.out.println("Failed to delete file: " + file.getName());
                    }
                }

            }
            default -> {
                response.setStatusCode(200);
            }
        }

    }

    private void handleCgi(HttpResource resource) {
        ProcessBuilder processBuilder = new ProcessBuilder(resource.getPath().toString());

        Map<String, String> env = processBuilder.environment();
        env.put("SERVER_PROTOCOL", "HTTP/1.1");
        request.getQueryString().ifPresent((queryString) -> env.put("QUERY_STRING", queryString));
        Map<Header, String> requestHeaders = request.getHeaders();
        for (Map.Entry<Header, String> requestHeader : requestHeaders.entrySet()) {
            env.put("HTTP_".concat(requestHeader.getKey().toString()), requestHeader.getValue());
        }

        byte[] body = request.getBody().getBytes();
        try {
            Process process = processBuilder.start();

            OutputStream outputStream = process.getOutputStream();
            outputStream.write(body, 0, body.length);
            outputStream.flush();
            outputStream.close();
            response.setStatusCode(200);
            writeCgiResponse(process);
            return;
        } catch (IOException e) {
            e.printStackTrace();
            response.setStatusCode(500);
            writeResponse();
            return;
        }
    }

    private String responseConcat(HttpRequest request) {
        String first = "Your content has been saved! \n Click <A href=\"";
        String second = "\">here</A> to view it.";
        return first + request.getID() + second;
    }

    private void writeResponse(){
        try (OutputStream outputStream = this.socket.getOutputStream()) {
            response.writeResponse(outputStream);
            outputStream.flush();
        } catch (IOException e) {
            // Handle the case where client closed the connection while server was writing
            // to it
            try {
                this.socket.close();
            } catch (IOException ex) {
                System.out.println("Error occurred when closing socket");
                ex.printStackTrace();
            }
        }
    }

    private void writeCgiResponse(Process process) throws IOException {
        try (OutputStream outputStream = this.socket.getOutputStream()) {
            response.writeMinimalCgiResponse(outputStream);
            process.getInputStream().transferTo(outputStream);
            outputStream.flush();
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

    private String getFileExtension(Path path) {
        String filename = path.toString();
        return Optional.of(filename).filter(f -> f.contains(".")).map(f -> f.substring(filename.lastIndexOf(".") + 1)).orElse("");
    }
}
