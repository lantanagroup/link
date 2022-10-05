package com.lantanagroup.link.datastore.auth;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.lantanagroup.link.auth.BasicAuthModel;
import com.lantanagroup.link.auth.OAuth2Helper;
import com.lantanagroup.link.config.datastore.DataStoreConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;


@Interceptor
public class UserInterceptor {
  protected static final Logger logger = LoggerFactory.getLogger(UserInterceptor.class);
  private DataStoreConfig config;

  public UserInterceptor(DataStoreConfig config) {
    this.config = config;
  }

  private void assertToken(String token) {
    if (this.config.getOauth() == null || StringUtils.isEmpty(this.config.getOauth().getIssuer()) || StringUtils.isEmpty(this.config.getOauth().getAuthJwksUrl())) {
      String msg = "oauth is not configured";
      logger.error(msg);
      throw new ConfigurationException(msg);
    }

    DecodedJWT jwt = OAuth2Helper.verifyToken(
            token,
            OAuth2Helper.TokenAlgorithmsEnum.RSA256,
            this.config.getOauth().getIssuer(),
            this.config.getOauth().getAuthJwksUrl());

    if(jwt == null) {
      String msg = "OAuth token certificate is unknown or invalid";
      logger.error(msg);
      throw new AuthenticationException(msg);
    }
  }

  private void assertBasic(String basic) {
    if (this.config.getBasicAuthUsers() == null) {
      String msg = "basicAuthUsers is not configured";
      logger.error(msg);
      throw new ConfigurationException(msg);
    }

    BasicAuthModel model = BasicAuthModel.getBasicAuth(basic);

    if (!this.config.getBasicAuthUsers().containsKey(model.getUsername())) {
      String msg = String.format("User %s not found in configuration", model.getUsername());
      logger.info(msg);
      throw new AuthenticationException(msg);
    }

    String p = this.config.getBasicAuthUsers().get(model.getUsername());

    if (!p.equals(model.getPassword())) {
      String msg = String.format("Credentials do not match configuration for user %", model.getUsername());
      logger.info(msg);
      throw new AuthenticationException(msg);
    }
  }

  @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
  public void intercept(RequestDetails requestDetails){
    String authHeader = requestDetails.getHeader("Authorization");
    String token = authHeader != null && authHeader.toLowerCase(Locale.ROOT).startsWith("bearer ") ? authHeader.substring(7) : null;
    String basic = authHeader != null && authHeader.toLowerCase(Locale.ROOT).startsWith("basic ") ? authHeader.substring(6) : null;

    if (authHeader == null) {
      String msg = "Authorization header not found, cannot continue processing request.";
      logger.error(msg);
      throw new AuthenticationException(msg);
    }

    if (StringUtils.isEmpty(token) && StringUtils.isEmpty(basic)) {
      String msg = "Authorization header is not \"basic\" or \"bearer\".";
      logger.error(msg);
      throw new AuthenticationException(msg);
    }

    if (StringUtils.isNotEmpty(token)) {
      this.assertToken(authHeader);
    }

    if (StringUtils.isNotEmpty(basic)) {
      this.assertBasic(authHeader);
    }
  }
}
