package com.lantanagroup.link.config.sender;


import com.lantanagroup.link.config.OAuthCredentialModes;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;

@Getter @Setter
public class FHIRSenderOAuthConfig {


    /**
     * <strong>oauth.credential-mode</strong>
     */
    private OAuthCredentialModes credentialMode;

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
        if (this.credentialMode != null) {
            switch (this.credentialMode) {
                case Client:
                    return Strings.isNotEmpty(this.tokenUrl) &&
                            Strings.isNotEmpty(this.username) &&
                            Strings.isNotEmpty(this.password) &&
                            Strings.isNotEmpty(this.scope);
                case Password:
                    return Strings.isNotEmpty(this.tokenUrl) &&
                            Strings.isNotEmpty(this.username) &&
                            Strings.isNotEmpty(this.password) &&
                            Strings.isNotEmpty(this.clientId) &&
                            Strings.isNotEmpty(this.scope);
            }
        }

        return false;
    }
}
