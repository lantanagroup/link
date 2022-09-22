package com.lantanagroup.link.api.auth;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.api.ApiConfig;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
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

  private boolean isSamePractitioner(Practitioner practitioner1, Practitioner practitioner2) {
    boolean same = true;
    String familyNamePractitioner1 = StringUtils.isNotBlank(practitioner1.getName().get(0).getFamily())?
            practitioner1.getName().get(0).getFamily():"";
    String familyNamePractitioner2 = StringUtils.isNotBlank(practitioner2.getName().get(0).getFamily())?
            practitioner2.getName().get(0).getFamily():"";
    String givenNamePractitioner1 = StringUtils.isNotBlank(practitioner1.getName().get(0).getGiven().get(0).toString())?
            practitioner1.getName().get(0).getGiven().get(0).toString():"";
    String givenNamePractitioner2 = StringUtils.isNotBlank(practitioner2.getName().get(0).getGiven().get(0).toString())?
            practitioner2.getName().get(0).getGiven().get(0).toString():"";
    String email1Value = StringUtils.isNotBlank(practitioner1.getTelecomFirstRep().getValue())?
            practitioner1.getTelecomFirstRep().getValue():"";
    String email2Value = StringUtils.isNotBlank(practitioner2.getTelecomFirstRep().getValue())?
            practitioner2.getTelecomFirstRep().getValue():"";
    String email1System = StringUtils.isNotBlank(practitioner1.getTelecomFirstRep().getSystem().name())?
            practitioner1.getTelecomFirstRep().getSystem().name():"";
    String email2System = StringUtils.isNotBlank(practitioner2.getTelecomFirstRep().getSystem().name())?
            practitioner2.getTelecomFirstRep().getSystem().name():"";
    if (!familyNamePractitioner1.equals(familyNamePractitioner2) || !givenNamePractitioner1.equals(givenNamePractitioner2) ||
            !email1System.equals(email2System) || !email1Value.equals(email2Value)) {
      same = false;
    }
    return same;
  }
}
