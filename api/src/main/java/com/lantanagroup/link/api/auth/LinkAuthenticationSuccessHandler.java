package com.lantanagroup.link.api.auth;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceGoneException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.lantanagroup.link.config.api.ApiConfig;
import org.hl7.fhir.r4.model.Practitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class LinkAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private static final Logger logger = LoggerFactory.getLogger(LinkAuthenticationSuccessHandler.class);
    private ApiConfig config;

    public LinkAuthenticationSuccessHandler(ApiConfig config) {
        this.config = config;
    }


    @Override
    public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException, ServletException {
        IGenericClient client = FhirContext.forR4().newRestfulGenericClient(config.getFhirServerStore());
        Practitioner practitioner = ((LinkCredentials) authentication.getPrincipal()).getPractitioner();
        try {
            client.read().resource(Practitioner.class).withId("2").execute();
        } catch (ResourceNotFoundException ex) {
            String msg = String.format("Practitioner Resource with identifier  \"%s\"  was not found on FHIR server \"%s\". It will be created.", practitioner.getId(), config.getFhirServerStore());
            logger.debug(msg);
            client.update().resource(practitioner).execute();
        } catch (ResourceGoneException ex) {
            String msg = String.format("Practitioner Resource with identifier  \"%s\"  was not found on FHIR server \"%s\". It will be created.", practitioner.getId(), config.getFhirServerStore());
            logger.debug(msg);
            client.update().resource(practitioner).execute();
        } catch (Exception ex) {
            String msg = String.format("Unable to retrieve practitioner with identifier  \"%s\"  from FHIR server \"%s\"", practitioner.getId(), config.getFhirServerStore());
            logger.error(msg);
            ex.printStackTrace();
        }
    }
}
