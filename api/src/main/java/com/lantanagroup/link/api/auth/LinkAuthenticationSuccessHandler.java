package com.lantanagroup.link.api.auth;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.api.ApiConfig;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LinkAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
  private static final Logger logger = LoggerFactory.getLogger(LinkAuthenticationSuccessHandler.class);
  private ApiConfig config;

  private FhirDataProvider provider;

  public LinkAuthenticationSuccessHandler(ApiConfig config) {
    this.config = config;
    this.provider = new FhirDataProvider(config.getDataStore());
  }

  @Override
  public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) {
    Practitioner practitioner = ((LinkCredentials) authentication.getPrincipal()).getPractitioner();

    try {

      Bundle bundle = provider.searchPractitioner(practitioner.getIdentifier().get(0).getValue());

      int size = bundle.getEntry().size();
      if (size == 0) {
        Resource resource = provider.createResource(practitioner);
        practitioner.setId(resource.getIdElement().getIdPart());

      } else {
        Practitioner foundPractitioner = ((Practitioner) bundle.getEntry().get(0).getResource());
        practitioner.setId(foundPractitioner.getId());
        if (!isSamePractitioner(practitioner, foundPractitioner)) {
          provider.updateResource(practitioner);
        }
      }
    } catch (ResourceNotFoundException ex) {
      String msg = String.format("Practitioner Resource with identifier \"%s\"  was not found on the data store. It will be created.", practitioner.getId());
      logger.debug(msg);
      provider.updateResource(practitioner);
    } catch (Exception ex) {
      String msg = String.format("Unable to retrieve practitioner with identifier \"%s\" from the data store", practitioner.getId());
      logger.error(msg);
      ex.printStackTrace();
    }
  }

  private String getFamilyName(Practitioner practitioner) {
    return practitioner.getNameFirstRep().getFamily();
  }

  private String getGivenName(Practitioner practitioner) {
    return practitioner.getNameFirstRep().getGivenAsSingleString();
  }

  private String getEmailAddress(Practitioner practitioner) {
    return practitioner.getTelecom().stream()
            .filter(telecom -> telecom.getSystem() == ContactPoint.ContactPointSystem.EMAIL)
            .map(ContactPoint::getValue)
            .findFirst()
            .orElse(null);
  }

  private boolean isSamePractitioner(Practitioner practitioner1, Practitioner practitioner2) {
    return StringUtils.equals(getFamilyName(practitioner1), getFamilyName(practitioner2))
            && StringUtils.equals(getGivenName(practitioner1), getGivenName(practitioner2))
            && StringUtils.equals(getEmailAddress(practitioner1), getEmailAddress(practitioner2));
  }
}
