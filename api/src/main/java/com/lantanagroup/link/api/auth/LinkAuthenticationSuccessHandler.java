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
import java.util.List;

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

  private String checkFamily (Practitioner practitioner) {
    return !practitioner.getName().isEmpty()?
            (StringUtils.isNotBlank(practitioner.getName().get(0).getFamily())?
                    practitioner.getName().get(0).getFamily():""):"";
  }

  private String checkGiven(Practitioner practitioner) {
    return !practitioner.getName().isEmpty()?
            (!practitioner.getName().get(0).getGiven().isEmpty()?
                    (StringUtils.isNotBlank(practitioner.getName().get(0).getGiven().get(0).toString())?
                            practitioner.getName().get(0).getGiven().get(0).toString():""):""):"";
  }

  private String checkEmailValue(Practitioner practitioner) {
    return !practitioner.getTelecomFirstRep().isEmpty()?
              (StringUtils.isNotBlank(practitioner.getTelecomFirstRep().getValue())?
              practitioner.getTelecomFirstRep().getValue():""):"";
  }

  private ContactPoint.ContactPointSystem checkEmailSystem(Practitioner practitioner) {
    return !practitioner.getTelecomFirstRep().isEmpty()?
              practitioner.getTelecomFirstRep().getSystem():ContactPoint.ContactPointSystem.NULL;
  }

  private boolean isSamePractitioner(Practitioner practitioner1, Practitioner practitioner2) {
   return (StringUtils.equals(checkFamily(practitioner1),checkFamily(practitioner2)) ||
            StringUtils.equals(checkGiven(practitioner1),checkGiven(practitioner2)) ||
            checkEmailSystem(practitioner1) == checkEmailSystem(practitioner2) ||
            StringUtils.equals(checkEmailValue(practitioner1),checkEmailValue(practitioner2)));
  }
}
