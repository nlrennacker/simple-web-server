package web.handler;

import web.authorization.AuthorizationChecker;
import web.request.HttpRequest;
import web.request.Header;
import web.resource.ConfigResource;
import web.resource.HttpResource;
import web.response.HTTPResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class Handler implements Runnable {

    private static final String DEFAULT_MIME_TYPE = "text/text";
    private static final String HT_ACCESS_FILENAME = ".htaccess";

    private final Socket socket;

    public Handler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            System.out.println("Connection Established: " + socket.getLocalSocketAddress());

            HttpRequest request = new HttpRequest(socket);
            HTTPResponse response = new HTTPResponse();
            response.addHeader("Connection", "close");

            if (!request.isValidRequest()) {
                response.setStatusCode(400);
                writeResponse(response);
                return;
            }

            HttpResource resource = new HttpResource(request);

            // side note for debugging, http auth is stored per browser, so if viewing auth
            // in browser window (as opposed to postman) you will not get the auth popup
            // unless you close and reopen the browser each time
            Path htAccessPath = Paths.get(resource.getPath().toAbsolutePath().getParent().toString(), HT_ACCESS_FILENAME);
            AuthorizationChecker authorizationChecker = new AuthorizationChecker(htAccessPath);
            AuthorizationChecker.AuthorizationResult authorizationResult = authorizationChecker
                    .checkAuthorization(request);
            if (authorizationResult.equals(AuthorizationChecker.AuthorizationResult.MISSING_AUTH)) {
                response.setStatusCode(401);
                response.addHeader("WWW-Authenticate", authorizationChecker.getWWWAuthenticateHeader());
                writeResponse(response);
                return;
            }
            if (authorizationResult.equals(AuthorizationChecker.AuthorizationResult.INVALID)) {
                response.setStatusCode(403);
                writeResponse(response);
                return;
            }

            if (Files.notExists(resource.getPath())) {
                response.setStatusCode(404);
                writeResponse(response);
                return;
            }

            switch (request.getMethod().toUpperCase()) {
                case "GET" -> {
                    if (request.hasHeader(Header.IFMODIFIEDSINCE) && resource.compareDateTime(request.getHeaderValue(Header.IFMODIFIEDSINCE))) {
                        response.setStatusCode(304);
                        writeResponse(response);
                        return;
                    }
                    String mimeType = ConfigResource.getMimeTypes().getMimeTypeForExtension(getFileExtension(resource.getPath()))
                            .orElse(DEFAULT_MIME_TYPE);
                    response.addHeader("Content-Type", mimeType);
                    response.setBody(Files.readAllBytes(resource.getPath()));
                    writeResponse(response);
                    return;
                }
                case "PUT" -> {

                }
                default -> {
                    response.setStatusCode(200);
                    writeResponse(response);
                    return;
                }
            }
        } catch (Exception e) {
            System.out.println("Error while handling request:");
            e.printStackTrace();
        }
    }


    private void writeResponse(HTTPResponse response) throws IOException {
        try (OutputStream outputStream = this.socket.getOutputStream()) {
            response.writeResponse(outputStream);
            outputStream.flush();
        } catch (SocketException e) {
            // Handle the case where client closed the connection while server was writing
            // to it
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
}
