package com.lantanagroup.link.config.sender;

import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;

@Getter @Setter
public class FHIRSenderOAuthConfig {
    /**
     * <strong>oauth.tokenUrl</strong><br>
     */
    private String tokenUrl;

    /**
     * <strong>oauth.clientId</strong><br>
     */
    private String clientId;

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
        return Strings.isNotEmpty(this.tokenUrl) &&
                Strings.isNotEmpty(this.username) &&
                Strings.isNotEmpty(this.password) &&
                Strings.isNotEmpty(this.scope);
    }
}
