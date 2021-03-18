package com.lantanagroup.nandina.api.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import javax.servlet.http.HttpServletRequest;

public class PreAuthTokenHeaderFilter extends AbstractPreAuthenticatedProcessingFilter {
  private String authHeaderName;

  public PreAuthTokenHeaderFilter(String authHeaderName) {
    this.authHeaderName = authHeaderName;
  }

  @Override
  protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
    return request.getHeader(authHeaderName);
  }

  @Override
  protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      DecodedJWT jwt = JWT.decode(authHeader.substring(7));
      return jwt;
    }

    return null;
  }
}
