package com.lantanagroup.link.api.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.auth.LinkCredentials;
import org.hl7.fhir.r4.model.*;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class PreAuthTokenHeaderFilter extends AbstractPreAuthenticatedProcessingFilter {
  private String authHeaderName;
  private LinkCredentials linkCredentials;

  public PreAuthTokenHeaderFilter (String authHeaderName, LinkCredentials linkCredentials) {
    this.authHeaderName = authHeaderName;
    this.linkCredentials = linkCredentials;
  }

  private static Practitioner toPractitioner (DecodedJWT jwt) {
    Practitioner practitioner = new Practitioner();
    // set Practitioner Id
    practitioner.getMeta().addTag(Constants.MainSystem, Constants.LinkUserTag, null);
    List identifiers = new ArrayList();
    Identifier identifier = new Identifier();
    identifier.setSystem(Constants.MainSystem);
    identifier.setValue(jwt.getSubject());
    identifiers.add(identifier);
    practitioner.setIdentifier(identifiers);
    String payload = jwt.getPayload();
    byte[] decodedBytes = Base64.getDecoder().decode(payload);
    String decodedString = new String(decodedBytes);
    JsonObject jsonObject = new JsonParser().parse(decodedString).getAsJsonObject();
    // set Practitioner Family Name and Given Name
    List<HumanName> list = new ArrayList<>();
    HumanName dst = new HumanName();
    if (jsonObject.has("family_name")) {
      dst.setFamily(jsonObject.get("family_name").toString());
    }
    if (jsonObject.has("given_name")) {
      List<StringType> givenNames = new ArrayList();
      givenNames.add(new StringType(jsonObject.get("given_name").toString()));
      dst.setGiven(givenNames);
    }
    list.add(dst);
    practitioner.setName(list);
    // set Practitioner Email
    if (jsonObject.has("email")) {
      List<ContactPoint> contactPointList = new ArrayList();
      ContactPoint email = new ContactPoint();
      email.setSystem(ContactPoint.ContactPointSystem.EMAIL);
      email.setValue(jsonObject.get("email").toString());
      contactPointList.add(email);
      practitioner.setTelecom(contactPointList);
    }
    return practitioner;
  }

  @Override
  protected Object getPreAuthenticatedPrincipal (HttpServletRequest request) {

    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      DecodedJWT jwt = JWT.decode(authHeader.substring(7));
      linkCredentials.setJwt(jwt);
      Practitioner practitioner = toPractitioner(jwt);
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
