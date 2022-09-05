package web.server.configuration.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigurationUtils {
    /**
     * Split configuration line into tokens by splitting on whitespace not surrounded by quotes.
     * @param line Configuration line, e.g. <code>"DocumentRoot "/dir/dir with spaces/""</code>
     * @return A list of tokens from the passed configuration line e.g. <code>{"DocumentRoot", "/dir/dir with spaces/"}</code>
     */
    public static List<String> splitConfigurationLineIntoTokens(String line) {
        List<String> tokens = new ArrayList<>();
        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        Matcher regexMatcher = regex.matcher(line);
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                // Add double-quoted string without the quotes
                tokens.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null) {
                // Add single-quoted string without the quotes
                tokens.add(regexMatcher.group(2));
            } else {
                // Add unquoted word
                tokens.add(regexMatcher.group());
            }
        }
        return tokens;
    }
}
