package web.server.configuration;

import web.server.configuration.utils.ConfigurationUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HttpdConf {
    private final Integer listen;
    private final String documentRoot;
    private final String logFile;
    private final Map<String, String> scriptAliases;
    private final String directoryIndex;

    public HttpdConf(List<String> config) {
        Integer tempListen = null;
        String tempDocumentRoot = null;
        String tempLogFile = null;
        Map<String, String> tempScriptAliases = null;
        String tempDirectoryIndex = null;

        for (String line : config) {
            List<String> tokens = ConfigurationUtils.splitConfigurationLineIntoTokens(line);
            String directive = tokens.get(0);
            switch (directive.toUpperCase()) {
                case "LISTEN" -> tempListen = Integer.parseInt(tokens.get(1));
                case "DOCUMENTROOT" -> tempDocumentRoot = tokens.get(1);
                case "LOGFILE" -> tempLogFile = tokens.get(1);
                case "SCRIPTALIAS" -> {
                    if (tempScriptAliases == null) {
                        tempScriptAliases = new HashMap<>();
                    }
                    tempScriptAliases.put(tokens.get(1), tokens.get(2));
                }
                case "DIRECTORYINDEX" -> tempDirectoryIndex = tokens.get(1);
                default ->
                    System.out.printf("Warning: HttpdConfiguration: Unrecognized or unsupported directive: %s%n", directive);
            }
        }

        listen = tempListen;
        documentRoot = tempDocumentRoot;
        logFile = tempLogFile;
        scriptAliases = tempScriptAliases;
        directoryIndex = tempDirectoryIndex;
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

    public Optional<Map<String, String>> getScriptAliases() {
        return Optional.ofNullable(scriptAliases);
    }

    public Optional<String> getDirectoryIndex() {
        return Optional.ofNullable(directoryIndex);
    }
}
