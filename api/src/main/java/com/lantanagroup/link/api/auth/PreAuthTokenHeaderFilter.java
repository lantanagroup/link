package com.lantanagroup.link.api.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.auth.LinkCredentials;
import org.hl7.fhir.r4.model.Practitioner;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import javax.servlet.http.HttpServletRequest;

public class PreAuthTokenHeaderFilter extends AbstractPreAuthenticatedProcessingFilter {
  private String authHeaderName;
  private LinkCredentials linkCredentials;

  public PreAuthTokenHeaderFilter (String authHeaderName, LinkCredentials linkCredentials) {
    this.authHeaderName = authHeaderName;
    this.linkCredentials = linkCredentials;
  }


  @Override
  protected Object getPreAuthenticatedPrincipal (HttpServletRequest request) {

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
