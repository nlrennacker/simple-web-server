package web.handler;

import web.request.HTTPRequest;
import web.request.Header;
import web.server.configuration.HtAccess;
import web.server.configuration.HtPassword;
import web.server.configuration.utils.ConfigurationReader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public class AuthorizationChecker {
    public enum AuthorizationResult {
        VALID,
        INVALID,
        MISSING_AUTH
    }

    private HtAccess htAccess;
    private String checkedUser;

    public AuthorizationChecker(Path htAccessPath) {
        try {
            if (htAccessPath.toFile().exists()) {
                this.htAccess = new HtAccess(ConfigurationReader.readConfiguration(htAccessPath.toString()));
            }
        } catch (IOException ignored) {}
    }

    public AuthorizationResult checkAuthorization(HTTPRequest request) throws IOException {
        if (htAccess == null) {
            return AuthorizationResult.VALID;
        } else {
            Map<String, String> htAccessConfiguration = this.htAccess.getConfiguration();
            switch (htAccessConfiguration.get("AuthType")) {
                case "Basic" -> {
                    String authorization = request.getHeaderValue(Header.AUTHORIZATION);
                    if (authorization == null || authorization.isEmpty()) {
                        return AuthorizationResult.MISSING_AUTH;
                    }
                    String[] tokenizedAuthInfo = HtPassword.tokenizeAuthInfo(authorization);
                    if (tokenizedAuthInfo.length >= 1) {
                        this.checkedUser = tokenizedAuthInfo[0];
                    }
                    HtPassword htPassword = new HtPassword(ConfigurationReader.readConfiguration(htAccessConfiguration.get("AuthUserFile")));
                    if (htPassword.isAuthorized(authorization)) {
                        return AuthorizationResult.VALID;
                    } else {
                        return AuthorizationResult.INVALID;
                    }
                }
                default -> {
                    System.out.format("Warning: Unsupported auth type %s", htAccessConfiguration.get("AuthType"));
                    return AuthorizationResult.INVALID;
                }
            }
        }
    }

    public String getWWWAuthenticateHeader() {
        Map<String, String> htAccessConfiguration = this.htAccess.getConfiguration();
        String authType = htAccessConfiguration.get("AuthType");
        switch (authType) {
            case "Basic" -> {
                return String.format(
                        "%s realm=\"%s\"",
                        authType,
                        this.htAccess.getConfiguration().get("AuthName")
                );
            }
            default -> {
                System.out.format("Warning: Unsupported auth type %s", authType);
                return "";
            }
        }
    }

    public Optional<String> getCheckedUser() {
        return Optional.ofNullable(this.checkedUser);
    }
}
