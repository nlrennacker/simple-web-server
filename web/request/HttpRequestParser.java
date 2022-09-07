package web.request;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class HttpRequestParser{

    private InputStream stream;
    private String fullRequest;
    private HTTPRequest request;

    public HttpRequestParser(Socket socket, HTTPRequest request) throws IOException{
        this.stream = socket.getInputStream();
        this.request = request;
        parseInput(stream);
    }

    private void parseInput(InputStream stream) throws IOException{

        fullRequest = readFullMessage(stream);

        splitHeader(fullRequest);

        switch (request.getMethod()){
            case "GET":
            case "HEAD":
            //request body is disregarded if there is one
            break;
            case "POST":
            case "PUT":
            //requests should have body but can be empty
            break;
            case "DELETE":
            //requests MAY have body
            break;
            default:
            //TODO
            //RAISE FLAG FOR UNSUPPORTED METHOD
        }
    }

    private void splitHeader(String fullHeader){
        String[] arr = fullHeader.split("\\s+");
        if(arr.length < 3){
            //TODO
            //CREATE FLAG FOR IMPROPER FORMAT
        } else {
            this.request.setHeader(arr);
        }
    }
    private String readFullMessage(InputStream stream) throws IOException {
            StringBuilder result = new StringBuilder();
            do {
                result.append((char) stream.read());
            } while (stream.available() > 0);
            return result.toString();
    }
    public String getFullRequest(){
        return fullRequest;
    }
}