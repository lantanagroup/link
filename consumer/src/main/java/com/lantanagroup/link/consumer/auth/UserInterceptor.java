package com.lantanagroup.link.consumer.auth;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.auth.OAuth2Helper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Interceptor
public class UserInterceptor {
  protected static final Logger logger = LoggerFactory.getLogger(UserInterceptor.class);
  private String issuer = "";
  private String jwksUrl = "";

  public UserInterceptor(String issuer, String jwksUrl) {
    this.issuer = issuer;
    this.jwksUrl = jwksUrl;
  }

  @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
  public void intercept(RequestDetails requestDetails){
    String authHeader = requestDetails.getHeader("Authorization");
    String token = authHeader != null && authHeader.startsWith("Bearer ") ? authHeader.substring("Bearer ".length()) : null;
    String[] apiKeys = requestDetails.getParameters().containsKey("_apiKey") ? requestDetails.getParameters().get("_apiKey") : null;
    String apiKey = apiKeys != null && apiKeys.length == 1 ? apiKeys[0] : null;

    if ((StringUtils.isEmpty(token) && StringUtils.isEmpty(apiKey)) || authHeader == null) {
      logger.error("api-key and Authorization header not found, cannot continue processing request.");
      throw new AuthenticationException();
    }

    try {
      DecodedJWT jwt = OAuth2Helper.verifyToken(authHeader, OAuth2Helper.TokenAlgorithmsEnum.RSA256, issuer, jwksUrl);

      if(jwt == null) {
        logger.error("OAuth token certificate is unknown or invalid");
        throw new AuthenticationException();
      }

      // retrieve all the roles for the user and stored them on requestDetails
      requestDetails.addParameter(Constants.Roles, OAuth2Helper.getUserRoles(jwt));
    }
    catch(Exception e) {
      throw new AuthenticationException(e.getMessage());
    }

  }
}
