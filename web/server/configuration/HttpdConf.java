package web.server.configuration;

import web.server.configuration.utils.ConfigurationUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HttpdConf {
    private static final Integer DEFAULT_LISTEN = 8080;

    private final Integer listen;
    private final String documentRoot;
    private final String logFile;
    private final Map<String, String> aliases;
    private final Map<String, String> scriptAliases;
    private final String accessFile;
    private final List<String> directoryIndexes;

    public HttpdConf(List<String> config) {
        int tempListen = DEFAULT_LISTEN;
        String tempDocumentRoot = null;
        String tempLogFile = null;
        Map<String, String> tempAliases = null;
        Map<String, String> tempScriptAliases = null;
        String tempAccessFile = null;
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
                case "ACCESSFILE" -> tempAccessFile = tokens.get(1);
                case "DIRECTORYINDEX" -> {
                    if (tempDirectoryIndexes == null) {
                        tempDirectoryIndexes = new ArrayList<>();
                    }
                    tempDirectoryIndexes.add(tokens.get(1));
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
    public Optional<String> getAccessFile() {
        return Optional.ofNullable(accessFile);
    }
    public Optional<List<String>> getDirectoryIndexes() {
        return Optional.ofNullable(directoryIndexes);
    }
}
