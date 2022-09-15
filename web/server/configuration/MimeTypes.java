package web.server.configuration;

import web.server.configuration.utils.ConfigurationUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MimeTypes {
    private final Map<String, String> mimeTypes;

    public MimeTypes(List<String> config) {
        mimeTypes = new HashMap<>();
        for (String line : config) {
            List<String> tokens = ConfigurationUtils.splitConfigurationLineIntoTokens(line);
            Iterator<String> iterator = tokens.iterator();
            String mimeType = iterator.next();
            while (iterator.hasNext()) {
                String extension = iterator.next();
                mimeTypes.put(extension, mimeType);
            }
        }
    }

    public Map<String, String> getMimeTypes() {
        return mimeTypes;
    }

    public Optional<String> getMimeTypeForExtension(String extension) {
        return Optional.ofNullable(mimeTypes.get(extension));
    }
}
