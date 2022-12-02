package com.lantanagroup.link.config.auth;


import com.lantanagroup.link.config.OAuthCredentialModes;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;

import java.util.Locale;

@Getter @Setter
public class LinkOAuthConfig {

    //TODO - Add RequestType: Submitting, LoadingMeasureDef, QueryingEHR

    /**
     * <strong>oauth.credential-mode</strong> Either "client", "password", or "sams-password"
     */
    private String credentialMode;

    /**
     * <strong>oauth.tokenUrl</strong><br>
     */
    private String tokenUrl;

    /**
     * <strong>oauth.clientId</strong><br>
     */
    private String clientId;

    /**
     * <strong>oauth.clientSecret</strong><br>
     */
    private String clientSecret;

    /**
     * <strong>oauth.username</strong><br>
     */
    private String username;

    /**
     * <strong>oauth.password</strong><br>
     */
    private String password;

    /**
     * <strong>oauth.scope</strong><br>
     */
    private String scope;

    public boolean hasCredentialProperties() {
        if (this.credentialMode != null) {
            switch (this.credentialMode.toLowerCase(Locale.ROOT)) {
                case "client":
                    return StringUtils.isNotEmpty(this.tokenUrl) &&
                            StringUtils.isNotEmpty(this.clientId) &&
                            StringUtils.isNotEmpty(this.password) &&
                            StringUtils.isNotEmpty(this.scope);
                case "password":
                    return StringUtils.isNotEmpty(this.tokenUrl) &&
                            StringUtils.isNotEmpty(this.username) &&
                            StringUtils.isNotEmpty(this.password) &&
                            StringUtils.isNotEmpty(this.clientId) &&
                            StringUtils.isNotEmpty(this.scope);
                case "sams-password":
                    return StringUtils.isNotEmpty(this.tokenUrl) &&
                            StringUtils.isNotEmpty(this.username) &&
                            StringUtils.isNotEmpty(this.password) &&
                            StringUtils.isNotEmpty(this.clientId) &&
                            StringUtils.isNotEmpty(this.clientSecret) &&
                            StringUtils.isNotEmpty(this.scope);
            }
        }

        return false;
    }
}
