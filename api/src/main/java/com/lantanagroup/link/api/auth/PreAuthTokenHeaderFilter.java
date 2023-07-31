package com.lantanagroup.link.api.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.api.ApiConfig;
import org.hl7.fhir.r4.model.Practitioner;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class PreAuthTokenHeaderFilter extends AbstractPreAuthenticatedProcessingFilter {
  private String authHeaderName;
  private LinkCredentials linkCredentials;
  private ApiConfig apiConfig;

  public PreAuthTokenHeaderFilter(String authHeaderName, LinkCredentials linkCredentials, ApiConfig apiConfig) {
    this.authHeaderName = authHeaderName;
    this.linkCredentials = linkCredentials;
    this.apiConfig = apiConfig;
  }

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    if (this.logger.isDebugEnabled()) {
      this.logger.debug("Checking secure context token: " + SecurityContextHolder.getContext().getAuthentication());
    }
    String ipAddress = FhirHelper.getRemoteAddress((HttpServletRequest) request);
    String authHeader = ((HttpServletRequest) request).getHeader("Authorization");
    //if configured, check to make sure the IP address of the request matches the IP address stored in the jwt token
    if (apiConfig.getCheckIpAddress() && authHeader != null && authHeader.startsWith("Bearer ")) {
      logger.info("Validating the requesting IP address against the token IP address.");
      DecodedJWT jwt = JWT.decode(authHeader.substring(7));
      if (jwt.getClaim("ip").isNull()) {
        throw new JWTVerificationException("IP Not in JWT, but check-ip-address is set to true");
      }
      if (!jwt.getClaim("ip").isNull() && !"0:0:0:0:0:0:0:1(0:0:0:0:0:0:0:1)".equals(ipAddress) && !jwt.getClaim("ip").asString().equals(ipAddress)) {
        throw new JWTVerificationException("IP Address does not match.");
      }
    }
    super.doFilter(request, response, chain);
  }

  @Override
  protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      DecodedJWT jwt = JWT.decode(authHeader.substring(7));
      linkCredentials.setJwt(jwt);
      Practitioner practitioner = FhirHelper.toPractitioner(jwt);
      linkCredentials.setPractitioner(practitioner);
      return linkCredentials;
    }
    return null;
  }

  @Override
  protected Object getPreAuthenticatedCredentials (HttpServletRequest request) {
    return request.getHeader(authHeaderName);
  }
}
