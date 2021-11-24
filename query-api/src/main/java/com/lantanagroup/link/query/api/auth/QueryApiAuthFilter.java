package com.lantanagroup.link.query.api.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.regex.*;

public class QueryApiAuthFilter extends AbstractPreAuthenticatedProcessingFilter {
  protected static final Logger logger = LoggerFactory.getLogger(QueryApiAuthFilter.class);

  public QueryApiAuthFilter(String expectedApiKey, String[] allowedRemotes) {
    this.setAuthenticationManager(new AuthenticationManager() {
      @Override
      public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        QueryApiAuthModel authModel = (QueryApiAuthModel) authentication.getPrincipal();

        //Initially had this running a check on the IP for properly formatted addresses but running the address through
        //the InetAddress.getByName method requires a try/catch block. So, it's effectively the same to just run each
        //through the method and let the catch block notify the user of incorrectly formatted IP addresses. (Error will
        //only show in the logs.)
        for (int x = 0; x < allowedRemotes.length; x++){
          try{
            allowedRemotes[x] = InetAddress.getByName(allowedRemotes[x]).toString();
            allowedRemotes[x] = allowedRemotes[x].substring(allowedRemotes[x].lastIndexOf("/"));
          }catch(UnknownHostException e){
            logger.error(e.getMessage());
            throw new BadCredentialsException(e.getMessage());
          }
        }

        if (!expectedApiKey.equals(authModel.getAuthorization())) {
          String msg = String.format("The API Key \"%s\" was not found or not the expected value.", authModel.getAuthorization());
          logger.error(msg);
          throw new BadCredentialsException(msg);
        }

        if (Arrays.asList(allowedRemotes).indexOf("/" + authModel.getRemoteAddress()) < 0) {
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
    QueryApiAuthModel authModel = new QueryApiAuthModel();
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
