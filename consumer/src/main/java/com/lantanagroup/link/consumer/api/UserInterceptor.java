package com.lantanagroup.link.consumer.api;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import com.lantanagroup.link.auth.OAuth2Helper;
import com.lantanagroup.link.config.api.ApiConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Interceptor
public class UserInterceptor {
  protected static final Logger logger = LoggerFactory.getLogger(UserInterceptor.class);

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

    Boolean authenticated = OAuth2Helper.validateAuthHeader(authHeader);
    if(!authenticated){
      logger.error("OAuth token certificate is unknown or invalid");
      throw new AuthenticationException();
    }

    //TODO: check if user is admin and put a flag in requestDetails.getUserData()
  }
}
