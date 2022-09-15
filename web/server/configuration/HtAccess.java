package web.server.configuration;

import web.server.configuration.utils.ConfigurationUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HtAccess {
    private final Map<String, String> configuration;

    public HtAccess(List<String> config) {
        this.configuration = new HashMap<>();
        for (String line : config) {
            List<String> tokens = ConfigurationUtils.splitConfigurationLineIntoTokens(line);
            this.configuration.put(tokens.get(0), tokens.get(1));
        }
    }

    public Map<String, String> getConfiguration() {
        return this.configuration;
    }
}
