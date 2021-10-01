package com.lantanagroup.link.config.sender;

import lombok.Getter;
import lombok.Setter;

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
}
