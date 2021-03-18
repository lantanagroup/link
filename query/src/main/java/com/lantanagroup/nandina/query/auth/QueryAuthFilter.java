package com.lantanagroup.nandina.query.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

public class QueryAuthFilter extends AbstractPreAuthenticatedProcessingFilter {
  protected static final Logger logger = LoggerFactory.getLogger(QueryAuthFilter.class);

  public QueryAuthFilter(String expectedApiKey, String[] allowedRemotes) {
    this.setAuthenticationManager(new AuthenticationManager() {
      @Override
      public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        QueryAuthModel authModel = (QueryAuthModel) authentication.getPrincipal();

        if (!expectedApiKey.equals(authModel.getAuthorization())) {
          String msg = String.format("The API Key \"%s\" was not found or not the expected value.", authModel.getAuthorization());
          logger.error(msg);
          throw new BadCredentialsException(msg);
        }

        if (Arrays.asList(allowedRemotes).indexOf(authModel.getRemoteAddress()) < 0) {
          String msg = String.format("The remote address \"%s\" was not in the configured allowed remote addresses", authModel.getRemoteAddress());
          logger.error(msg);
          throw new BadCredentialsException(msg);
        }

        authentication.setAuthenticated(true);
        return authentication;
      }
    });
  }

  @Override
  protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
    QueryAuthModel authModel = new QueryAuthModel();
    String authorization = request.getHeader("Authorization");

    if (authorization != null && authorization.toLowerCase().startsWith("key ")) {
      authModel.setAuthorization(authorization.substring(4));
    }

    authModel.setRemoteAddress(request.getRemoteAddr());

    return authModel;
  }

  @Override
  protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
    return "N/A";
  }
}
