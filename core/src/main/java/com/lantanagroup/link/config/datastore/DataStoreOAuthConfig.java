package com.lantanagroup.link.config.datastore;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DataStoreOAuthConfig {
  /***
   <strong>datastore.oauth.issuer</strong><br>This issuer is used during token validation to ensure that the JWT has been issued by a trusted system
   */
  @Getter
  private String issuer;

  /**
   * <strong>datastore.oauth.algorithm</strong><br>The algorithm used by the identity provider to sign the jwt token
   */
  @Getter
  private String algorithm;

  /**
   * <strong>datastore.oauth.tokenVerificationClass</strong><br>The class configured to verify a jwt token in the api
   */
  @Getter
  private String tokenVerificationClass;

  /***
   <strong>datastore.oauth.authJwksUrl</strong><br>The url endpoint for certs from the identity provider, which is used to verify any JSON Web Token (JWT)
   */
  @Getter
  private String authJwksUrl;

  /**
   * <strong>datastore.oauth.tokenValidationEndpoint</strong><br>The class configured to verify a jwt token in the api
   */
  @Getter
  private String tokenValidationEndpoint;

}
