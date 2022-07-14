package com.lantanagroup.link.datastore.auth;

import com.auth0.jwt.JWT;
import com.lantanagroup.link.auth.LinkCredentials;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import javax.servlet.http.HttpServletRequest;

public class PreAuthTokenHeaderFilter extends AbstractPreAuthenticatedProcessingFilter {
  private String authHeaderName;

  public PreAuthTokenHeaderFilter(String authHeaderName) {
    this.authHeaderName = authHeaderName;
  }


  @Override
  protected Object getPreAuthenticatedPrincipal (HttpServletRequest request) {

    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      return  JWT.decode(authHeader.substring(7));
    }
    return null;
  }

  @Override
  protected Object getPreAuthenticatedCredentials (HttpServletRequest request) {
    return request.getHeader(authHeaderName);
  }
}
