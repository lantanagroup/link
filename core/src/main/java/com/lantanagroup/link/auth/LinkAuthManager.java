package com.lantanagroup.link.auth;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import javax.validation.constraints.NotNull;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Locale;

@RequiredArgsConstructor
public class LinkAuthManager implements AuthenticationManager {
  private static final Logger logger = LoggerFactory.getLogger(LinkAuthManager.class);
  private final String issuer;
  private final String alogrithm;
  private final String jwksUrl;
  private final String tokenVerificationClass;
  private final HashMap<String, String> basicUsers;
  private final String tokenValidationEndpoint;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {

    try {
      ITokenValidator tokenVerifierClass;
      Class<?> verifierClass = Class.forName(tokenVerificationClass);
      Constructor<?> verifierClassConstructor = verifierClass.getConstructor();
      tokenVerifierClass = (ITokenValidator) verifierClassConstructor.newInstance();


      String authHeader = (String) authentication.getCredentials();

      Boolean allowBearer = StringUtils.isNotEmpty(this.issuer) && StringUtils.isNotEmpty(this.jwksUrl);
      Boolean allowBasic = this.basicUsers != null && !this.basicUsers.isEmpty();

      if (authHeader == null) {
        throw new BadCredentialsException("Authorization is required");
      }

      Boolean hasBearer = authHeader.toLowerCase(Locale.ROOT).startsWith("bearer ");
      Boolean hasBasic = authHeader.toLowerCase(Locale.ROOT).startsWith("basic" );

      if (allowBearer && hasBearer) {
         authentication.setAuthenticated(tokenVerifierClass.verifyToken(authHeader, alogrithm, issuer, jwksUrl, tokenValidationEndpoint));
      } else if (allowBasic && hasBasic) {
        BasicAuthModel basicAuthModel = BasicAuthModel.getBasicAuth(authHeader);
        String p = this.basicUsers.get(basicAuthModel.getUsername());
        authentication.setAuthenticated(p != null && p.equals(basicAuthModel.getPassword()));
      }

    } catch (ClassNotFoundException e) {
      logger.error("Invalid token validation configuration.");
    } catch (InvocationTargetException e) {
      logger.error("Invalid token validation configuration.");
    } catch (NoSuchMethodException e) {
      logger.error("Invalid token validation configuration.");
    } catch (InstantiationException e) {
      logger.error("Invalid token validation configuration.");
    } catch (IllegalAccessException e) {
      logger.error("Invalid token validation configuration.");
    }

    return authentication;
  }
}
