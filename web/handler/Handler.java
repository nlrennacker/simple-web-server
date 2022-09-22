package web.handler;

import web.request.HTTPRequest;
import web.request.Header;
import web.response.HTTPResponse;
import web.server.configuration.HttpdConf;
import web.server.configuration.MimeTypes;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

public class Handler implements Runnable {

    private static final String DEFAULT_DIRECTORY_INDEX = "index.html";
    private static final String DEFAULT_MIME_TYPE = "text/text";
    private static final String HT_ACCESS_FILENAME = ".htaccess";

    private final Socket socket;
    private final HttpdConf httpdConf;
    private final MimeTypes mimeTypes;

    public Handler(Socket socket, HttpdConf httpdConf, MimeTypes mimeTypes) {
        this.socket = socket;
        this.httpdConf = httpdConf;
        this.mimeTypes = mimeTypes;
    }

    @Override
    public void run() {
        try {
            System.out.println("Connection Established: " + socket.getLocalSocketAddress());

            HTTPRequest request = new HTTPRequest(socket);
            HTTPResponse response = new HTTPResponse();
            response.addHeader("Connection", "close");

            if (!request.isValidRequest()) {
                response.setStatusCode(400);
                writeResponse(response);
                return;
            }

            // Resolve request URI to an absolute path
            String requestUri = request.getID();
            boolean isScriptAliased = false;
            if (httpdConf.getScriptAliases().isPresent()) {
                for (Map.Entry<String, String> scriptAlias : httpdConf.getScriptAliases().get().entrySet()) {
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
            AuthorizationChecker authorizationChecker = new AuthorizationChecker(htAccessPath);
            AuthorizationChecker.AuthorizationResult authorizationResult = authorizationChecker.checkAuthorization(request);
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

            if (Files.notExists(requestPath)) {
                response.setStatusCode(404);
                writeResponse(response);
                return;
            }

            if (isScriptAliased) {
                ProcessBuilder processBuilder = new ProcessBuilder(requestPath.toString());

                Map<String, String> env = processBuilder.environment();
                env.put("HTTP_VERSION", "1.1");
                String queryString = request.getID().lastIndexOf('?') == -1 ?
                        "" :
                        request.getID().substring(request.getID().lastIndexOf('?'));
                env.put("QUERY_STRING", queryString);
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
                    writeCgiResponse(response, process);
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    response.setStatusCode(500);
                    writeResponse(response);
                    return;
                }
            }

            switch (request.getMethod().toUpperCase()) {
                case "GET" -> {
                    String mimeType = this.mimeTypes.getMimeTypeForExtension(getFileExtension(requestPath)).orElse(DEFAULT_MIME_TYPE);
                    response.addHeader("Content-Type", mimeType);
                    response.setBody(Files.readAllBytes(requestPath));
                    writeResponse(response);
                    return;
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

            try {
                HTTPResponse response = new HTTPResponse();
                response.addHeader("Connection", "close");
                response.setStatusCode(500);
                writeResponse(response);
            } catch (IOException ignored) {}
        }
    }

    private void writeResponse(HTTPResponse response) throws IOException {
        try (OutputStream outputStream = this.socket.getOutputStream()) {
            response.writeResponse(outputStream);
            outputStream.flush();
        } catch (SocketException e) {
            // Handle the case where client closed the connection while server was writing to it
            this.socket.close();
        }
    }

    private void writeCgiResponse(HTTPResponse response, Process process) throws IOException {
        try (OutputStream outputStream = this.socket.getOutputStream()) {
            response.writeMinimalCgiResponse(outputStream);
            process.getInputStream().transferTo(outputStream);
            outputStream.flush();
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
}
