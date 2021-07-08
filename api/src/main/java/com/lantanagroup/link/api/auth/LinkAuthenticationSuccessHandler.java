package com.lantanagroup.link.api.auth;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.config.api.ApiConfig;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Practitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LinkAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
  private static final Logger logger = LoggerFactory.getLogger(LinkAuthenticationSuccessHandler.class);
  private ApiConfig config;

  public LinkAuthenticationSuccessHandler (ApiConfig config) {
    this.config = config;
  }


  @Override
  public void onAuthenticationSuccess (HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) {
    IGenericClient fhirStoreClient = FhirContext.forR4().newRestfulGenericClient(config.getFhirServerStore());
    Practitioner practitioner = ((LinkCredentials) authentication.getPrincipal()).getPractitioner();
    try {
      Bundle bundle = fhirStoreClient
              .search()
              .forResource(Practitioner.class)
              .where(Practitioner.IDENTIFIER.exactly().systemAndValues(Constants.MainSystem, practitioner.getIdentifier().get(0).getValue()))
              .returnBundle(Bundle.class)
              .cacheControl(new CacheControlDirective().setNoCache(true))
              .execute();
      int size = bundle.getEntry().size();
      if (size == 0) {
        MethodOutcome outcome = fhirStoreClient.create().resource(practitioner).execute();
        if (outcome.getCreated() && outcome.getResource() != null) {
          practitioner.setId(outcome.getResource().getIdElement().getIdPart());
        }
      } else {
        Practitioner foundPractitioner = ((Practitioner) bundle.getEntry().get(0).getResource());
        practitioner.setId(foundPractitioner.getId());
        if (!isSamePractitioner(practitioner, foundPractitioner)) {
          fhirStoreClient.update().resource(practitioner).execute();
        }
      }
    } catch (ResourceNotFoundException ex) {
      String msg = String.format("Practitioner Resource with identifier  \"%s\"  was not found on FHIR server \"%s\". It will be created.", practitioner.getId(), config.getFhirServerStore());
      logger.debug(msg);
      fhirStoreClient.update().resource(practitioner).execute();
    } catch (Exception ex) {
      String msg = String.format("Unable to retrieve practitioner with identifier  \"%s\"  from FHIR server \"%s\"", practitioner.getId(), config.getFhirServerStore());
      logger.error(msg);
      ex.printStackTrace();
    }
  }

  private boolean isSamePractitioner (Practitioner practitioner1, Practitioner practitioner2) {
    boolean same = true;
    String familyNamePractitioner1 = practitioner1.getName().get(0).getFamily();
    String familyNamePractitioner2 = practitioner2.getName().get(0).getFamily();
    String givenNamePractitioner1 = practitioner1.getName().get(0).getGiven().get(0).toString();
    String givenNamePractitioner2 = practitioner2.getName().get(0).getGiven().get(0).toString();
    String email1Value = practitioner1.getTelecomFirstRep().getValue();
    String email2Value = practitioner2.getTelecomFirstRep().getValue();
    String email1System = practitioner1.getTelecomFirstRep().getSystem().name();
    String email2System = practitioner2.getTelecomFirstRep().getSystem().name();
    if (!familyNamePractitioner1.equals(familyNamePractitioner2) || !givenNamePractitioner1.equals(givenNamePractitioner2) ||
            !email1System.equals(email2System) || !email1Value.equals(email2Value)) {
      same = false;
    }
    return same;
  }
}
