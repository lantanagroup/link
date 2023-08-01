package com.lantanagroup.link.config.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LinkAuthManager {

    /**
     * <strong>issuer</strong><br>This issuer is used during token validation to ensure that the JWT has been issued by a trusted system
     */
    private String issuer;

    /**
     * <strong>auth-jwks-url</strong><br>The url endpoint for certs from the identity provider, which is used to verify any JSON Web Token (JWT)
     */
    private String authJwksUrl;

    /**
     * <strong>algorithm</strong><br>The algorithm used by the identity provider to sign the jwt token
     */
    private String algorithm;

    /**
     * <strong>tokenVerificationClass</strong><br>The class configured to verify a jwt token in the api
     */
    private String tokenVerificationClass;

    /**
     * <strong>tokenValidationEndpoint</strong><br>The url for the identity provider's token validation endpoint
     */
    private String tokenValidationEndpoint;
}
