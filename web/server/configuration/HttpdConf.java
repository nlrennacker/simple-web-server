package web.server.configuration;

import web.server.configuration.utils.ConfigurationUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HttpdConf {
    private final Integer listen;
    private final String documentRoot;
    private final String logFile;
    private final Map<String, String> aliases;
    private final Map<String, String> scriptAliases;
    private final List<String> accessFile;
    private final List<String> directoryIndexes;

    public HttpdConf(List<String> config) {
        Integer tempListen = null;
        String tempDocumentRoot = null;
        String tempLogFile = null;
        Map<String, String> tempAliases = null;
        Map<String, String> tempScriptAliases = null;
        List<String> tempAccessFile = null;
        List<String> tempDirectoryIndexes = null;

        for (String line : config) {
            List<String> tokens = ConfigurationUtils.splitConfigurationLineIntoTokens(line);
            String directive = tokens.get(0);
            switch (directive.toUpperCase()) {
                case "LISTEN" -> tempListen = Integer.parseInt(tokens.get(1));
                case "DOCUMENTROOT" -> tempDocumentRoot = tokens.get(1);
                case "LOGFILE" -> tempLogFile = tokens.get(1);
                case "ALIAS" -> {
                    if (tempAliases == null) {
                        tempAliases = new HashMap<>();
                    }
                    tempAliases.put(tokens.get(1), tokens.get(2));
                }
                case "SCRIPTALIAS" -> {
                    if (tempScriptAliases == null) {
                        tempScriptAliases = new HashMap<>();
                    }
                    tempScriptAliases.put(tokens.get(1), tokens.get(2));
                }
                case "ACCESSFILE" -> {
                    if (tempAccessFile == null) {
                        tempAccessFile = new ArrayList<>();
                    }
                    tempAccessFile.addAll(tokens.subList(1, tokens.size()));
                }
                case "DIRECTORYINDEX" -> {
                    // https://httpd.apache.org/docs/current/mod/mod_dir.html
                    // A single argument of "disabled" prevents mod_dir from searching for an index.
                    // An argument of "disabled" will be interpreted literally if it has any arguments before or after
                    // it, even if they are "disabled" as well.
                    if (tokens.size() == 2 && tokens.get(1).equals("disabled")) {
                        tempDirectoryIndexes = null;
                        break;
                    }
                    if (tempDirectoryIndexes == null) {
                        tempDirectoryIndexes = new ArrayList<>();
                    }
                    tempDirectoryIndexes.addAll(tokens.subList(1, tokens.size()));
                }
                default ->
                    System.out.printf("Warning: HttpdConfiguration: Unrecognized or unsupported directive: %s%n", directive);
            }
        }

        listen = tempListen;
        documentRoot = tempDocumentRoot;
        logFile = tempLogFile;
        aliases = tempAliases;
        scriptAliases = tempScriptAliases;
        accessFile = tempAccessFile;
        directoryIndexes = tempDirectoryIndexes;
    }

    public Optional<Integer> getListen() {
        return Optional.ofNullable(listen);
    }
    public Optional<String> getDocumentRoot() {
        return Optional.ofNullable(documentRoot);
    }
    public Optional<String> getLogFile() {
        return Optional.ofNullable(logFile);
    }
    public Optional<Map<String, String>> getAliases() {
        return Optional.ofNullable(aliases);
    }
    public Optional<Map<String, String>> getScriptAliases() {
        return Optional.ofNullable(scriptAliases);
    }
    public Optional<List<String>> getAccessFile() {
        return Optional.ofNullable(accessFile);
    }
    public Optional<List<String>> getDirectoryIndexes() {
        return Optional.ofNullable(directoryIndexes);
    }
}
