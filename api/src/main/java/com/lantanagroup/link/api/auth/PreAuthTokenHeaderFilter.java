package com.lantanagroup.link.api.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.Hasher;
import com.lantanagroup.link.auth.ITokenValidator;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Base64;
import java.util.Locale;

public class PreAuthTokenHeaderFilter extends AbstractPreAuthenticatedProcessingFilter {
  private static final String EMAIL_CLAIM = "email";
  private static final Logger logger = LoggerFactory.getLogger(PreAuthTokenHeaderFilter.class);

  private final String authHeaderName;
  private final ApiConfig apiConfig;
  private final SharedService sharedService;

  public PreAuthTokenHeaderFilter(String authHeaderName, ApiConfig apiConfig, SharedService sharedService) {
    this.authHeaderName = authHeaderName;
    this.apiConfig = apiConfig;
    this.sharedService = sharedService;
  }

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    logger.debug("Checking secure context token: " + SecurityContextHolder.getContext().getAuthentication());

    String ipAddress = FhirHelper.getRemoteAddress((HttpServletRequest) request);
    String authHeader = ((HttpServletRequest) request).getHeader("Authorization");

    //if configured, check to make sure the IP address of the request matches the IP address stored in the jwt token
    if (apiConfig.getCheckIpAddress() && authHeader != null && authHeader.startsWith("Bearer ")) {
      logger.debug("Validating the requesting IP address against the token IP address.");
      DecodedJWT jwt = JWT.decode(authHeader.substring(7));

      if (!jwt.getClaim("ip").isNull() && !"127.0.0.1(127.0.0.1)".equals(ipAddress) && !"0:0:0:0:0:0:0:1(0:0:0:0:0:0:0:1)".equals(ipAddress) && !jwt.getClaim("ip").asString().equals(ipAddress)) {
        throw new JWTVerificationException("IP Address does not match.");
      }
    }

    super.doFilter(request, response, chain);
  }


  private LinkCredentials handleToken(String authHeader) {
    try {
      ITokenValidator tokenVerifierClass;
      Class<?> verifierClass = Class.forName(this.apiConfig.getTokenVerificationClass());
      Constructor<?> verifierClassConstructor = verifierClass.getConstructor();
      tokenVerifierClass = (ITokenValidator) verifierClassConstructor.newInstance();

      boolean valid = tokenVerifierClass.verifyToken(authHeader, this.apiConfig.getAlgorithm(), this.apiConfig.getIssuer(), this.apiConfig.getAuthJwksUrl(), this.apiConfig.getTokenValidationEndpoint());

      if (valid) {
        DecodedJWT jwt = JWT.decode(authHeader.substring(7));
        String email = jwt.getClaim(EMAIL_CLAIM).asString();
        if (StringUtils.isEmpty(email)) {
          logger.error("No email claim in JWT");
          return null;
        }
        User found = this.sharedService.findUser(email);
        if (found == null) {
          logger.error("Email not found: {}", email);
          return null;
        }

        LinkCredentials linkCredentials = new LinkCredentials();
        linkCredentials.setUser(found);
        linkCredentials.setJwt(jwt);
        return linkCredentials;
      }
    } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException | InstantiationException |
             IllegalAccessException e) {
      logger.error("Invalid token validation configuration.");
    }

    return null;
  }

  private LinkCredentials handleBasic(String authHeader) {
    byte[] authBytes = Base64.getDecoder().decode(authHeader.substring(6));
    String[] split = new String(authBytes).split(":");

    if (split.length != 2) {
      logger.warn("Expected basic auth header to have two parts");
      return null;
    }

    String email = split[0];
    String password = split[1];
    User found = this.sharedService.findUser(email);

    if (found == null) {
      logger.error("User with email {} not found", email);
      return null;
    }

    if (!found.hasPassword()) {
      logger.error("User with email {} has no password", email);
      return null;
    }

    if (!Hasher.check(password, found.getPasswordSalt(), found.getPasswordHash())) {
      logger.error("Password for user with email {} not correct", email);
      return null;
    }

    LinkCredentials linkCredentials = new LinkCredentials();
    linkCredentials.setUser(found);
    return linkCredentials;
  }

  @Override
  protected Object getPreAuthenticatedPrincipal(HttpServletRequest httpServletRequest) {
    String authHeader = httpServletRequest.getHeader("Authorization");

    if (StringUtils.isEmpty(authHeader)) {
      return null;
    }

    Boolean allowBearer = StringUtils.isNotEmpty(this.apiConfig.getIssuer()) && StringUtils.isNotEmpty(this.apiConfig.getAuthJwksUrl());
    Boolean hasBearer = authHeader.toLowerCase(Locale.ROOT).startsWith("bearer ");
    boolean hasBasic = authHeader.toLowerCase(Locale.ROOT).startsWith("basic");

    if (allowBearer && hasBearer) {
      return this.handleToken(authHeader);
    } else if (hasBasic) {
      return this.handleBasic(authHeader);
    }

    return null;
  }

  @Override
  protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
    return request.getHeader(authHeaderName);
  }
}
