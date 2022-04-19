package com.lantanagroup.link.api.auth;

import com.lantanagroup.link.auth.OAuth2Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;


public class LinkAuthManager implements AuthenticationManager {
  private static final Logger logger = LoggerFactory.getLogger(LinkAuthManager.class);
  private String issuer = "";
  private String jwksUrl = "";
  public LinkAuthManager(String issuer, String jwksUrl) {
    this.issuer = issuer;
    this.jwksUrl = jwksUrl;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String authHeader = (String) authentication.getCredentials();

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new BadCredentialsException("This REST operation requires a Bearer Authorization header.");
    }

    authentication.setAuthenticated(OAuth2Helper.verifyToken(authHeader, OAuth2Helper.TokenAlgorithmsEnum.RSA256, this.issuer, this.jwksUrl) != null ? true : false);

    return authentication;
  }
}
