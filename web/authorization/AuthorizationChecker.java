package web.authorization;

import web.request.HttpRequest;
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
    /**
     * AuthorizationChecker constructor: creates respective HtAccess object in order to parse authentication.
     * @param htAccessPath A path to the htAccess file which must be included in the file system for authentication.
     */
    public AuthorizationChecker(Path htAccessPath) {
        try {
            if (htAccessPath.toFile().exists()) {
                this.htAccess = new HtAccess(ConfigurationReader.readConfiguration(htAccessPath.toString()));
            }
        } catch (IOException ignored) {}
    }

    /**
     * Checks the authorization of the request and returns a valid or invalid response based on Authentication match
     * @param request An HTTP request with the authorization header
     * @return an AuthorizationResult (enum): VALID if match, INVALID if not
     * @throws IOException if authorization type is unsupported then INVALID is returned
     */
    public AuthorizationResult checkAuthorization(HttpRequest request) throws IOException {
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

    /**
     * @return Returns the respective Authentication Header in specific format -> [authorizationType] realm=\[authorizationName]\
     */
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

    /**
     * 
     * @return nullable Optional string of user
     */
    public Optional<String> getCheckedUser() {
        return Optional.ofNullable(this.checkedUser);
    }
}
