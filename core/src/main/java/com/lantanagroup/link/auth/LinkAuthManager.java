package com.lantanagroup.link.auth;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.HashMap;
import java.util.Locale;

@RequiredArgsConstructor
public class LinkAuthManager implements AuthenticationManager {
  private static final Logger logger = LoggerFactory.getLogger(LinkAuthManager.class);
  private final String issuer;
  private final String jwksUrl;
  private final HashMap<String, String> basicUsers;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String authHeader = (String) authentication.getCredentials();

    Boolean allowBearer = StringUtils.isNotEmpty(this.issuer) && StringUtils.isNotEmpty(this.jwksUrl);
    Boolean allowBasic = this.basicUsers != null && !this.basicUsers.isEmpty();

    if (authHeader == null) {
      throw new BadCredentialsException("Authorization is required");
    }

    Boolean hasBearer = authHeader.toLowerCase(Locale.ROOT).startsWith("bearer ");
    Boolean hasBasic = authHeader.toLowerCase(Locale.ROOT).startsWith("basic" );

    if (allowBearer && hasBearer) {
      authentication.setAuthenticated(OAuth2Helper.verifyToken(authHeader, OAuth2Helper.TokenAlgorithmsEnum.RSA256, this.issuer, this.jwksUrl) != null ? true : false);
    } else if (allowBasic && hasBasic) {
      BasicAuthModel basicAuthModel = BasicAuthModel.getBasicAuth(authHeader);
      String p = this.basicUsers.get(basicAuthModel.getUsername());
      authentication.setAuthenticated(p != null && p.equals(basicAuthModel.getPassword()));
    }

    return authentication;
  }
}
