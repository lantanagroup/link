package com.lantanagroup.link.datastore.auth;

import com.auth0.jwt.JWT;
import com.lantanagroup.link.auth.BasicAuthModel;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

public class PreAuthTokenHeaderFilter extends AbstractPreAuthenticatedProcessingFilter {
  private String authHeaderName;

  public PreAuthTokenHeaderFilter(String authHeaderName) {
    this.authHeaderName = authHeaderName;
  }

  @Override
  protected Object getPreAuthenticatedPrincipal (HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");

    if (authHeader != null) {
      if (authHeader.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
        return JWT.decode(authHeader.substring(7));
      } else if (authHeader.toLowerCase(Locale.ROOT).startsWith("basic ")) {
        return BasicAuthModel.getBasicAuth(authHeader);
      }
    }

    return null;
  }

  @Override
  protected Object getPreAuthenticatedCredentials (HttpServletRequest request) {
    return request.getHeader(authHeaderName);
  }
}
