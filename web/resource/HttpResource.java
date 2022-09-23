package web.resource;

import web.request.HttpRequest;

import javax.print.attribute.DateTimeSyntax;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class HttpResource {
    private static final String DEFAULT_DIRECTORY_INDEX = "index.html";
    private Path requestPath;
    private final HttpRequest request;

    private static int fileCount;

    public HttpResource(HttpRequest request) {
        this.request = request;
        this.findPath();
    }

    private void findPath() {
        // Resolve request URI to an absolute path
        String requestUri = request.getID();
        boolean isScriptAliased = false;
        if (ConfigResource.getHttpdConf().getScriptAliases().isPresent()) {
            for (Map.Entry<String, String> scriptAlias : ConfigResource.getHttpdConf().getScriptAliases().get().entrySet()) {
                if (requestUri.contains(scriptAlias.getKey())) {
                    isScriptAliased = true;
                    requestUri = requestUri.replaceFirst(Pattern.quote(scriptAlias.getKey()), Pattern.quote(scriptAlias.getValue()));
                    break;
                }
            }
        }
        if (!isScriptAliased) {
            requestPath = Paths.get(ConfigResource.getHttpdConf().getDocumentRoot().get(), requestUri);
        } else {
            requestPath = Paths.get(requestUri);
        }
        if (Files.isDirectory(requestPath)) {
            requestPath = Paths.get(requestPath.toString(), ConfigResource.getHttpdConf().getDirectoryIndex().orElse(DEFAULT_DIRECTORY_INDEX));
        }
    }

    public Path getPath() {
        return requestPath;
    }

    /**
     * Compares the supplied date time with the cached file date time
     *
     * @param unformattedDate
     * @return boolean TRUE if the supplied date time is before the file date time and FALSE if it is after or if it failed to match
     */
    public boolean compareDateTime(String unformattedDate) {

        DateTimeFormatter  formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(ZoneId.of("GMT"));

        try {
            ZonedDateTime dateToCompare = ZonedDateTime.parse(unformattedDate, formatter);

            ZonedDateTime fileDateTime = Instant.ofEpochMilli(requestPath.toFile().lastModified()).atZone(ZoneId.of(ZonedDateTime.now().getOffset().toString()));

            System.out.println(dateToCompare + "\n" + fileDateTime);

            return dateToCompare.isAfter(fileDateTime);

        } catch (DateTimeParseException e) {
            System.out.println("Illegal Date Format");
            e.printStackTrace();
            return false;
        }
    }

    public String getFileDateTimeToString(){
        //this is monstrous but it works
        return DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(ZoneId.of("GMT")).format(Instant.ofEpochMilli(requestPath.toFile().lastModified()).atZone(ZoneId.of(ZonedDateTime.now().getOffset().toString())));
    }


}
