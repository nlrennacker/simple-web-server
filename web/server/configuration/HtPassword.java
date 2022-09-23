package web.server.configuration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HtPassword {
    private final Map<String, String> passwords;

    public HtPassword(List<String> config) {
        this.passwords = new HashMap<>();
        for (String line : config) {
            String[] tokens = line.split(":");
            if (tokens.length == 2) {
                this.passwords.put(tokens[0], tokens[1].replace("{SHA}", "").trim());
            }
        }
    }

    public static String[] tokenizeAuthInfo(String authInfo) {
        authInfo = authInfo.replaceFirst("^Basic ", "");
        String credentials = new String(
                Base64.getDecoder().decode(authInfo),
                StandardCharsets.UTF_8
        );
        String[] tokens = credentials.split(":");
        return tokens;
    }

    public boolean isAuthorized(String authInfo) {
        if (authInfo == null) {
            return false;
        }
        String[] tokens = HtPassword.tokenizeAuthInfo(authInfo);
        if (tokens.length != 2) {
            return false;
        }
        return verifyPassword(tokens[0], tokens[1]);
    }

    private boolean verifyPassword(String username, String password) {
        if (!passwords.containsKey(username)) {
            return false;
        }
        return passwords.get(username).equals(encryptClearPassword(password));
    }

    private String encryptClearPassword(String password) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            byte[] result = messageDigest.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            return "";
        }
    }
}
