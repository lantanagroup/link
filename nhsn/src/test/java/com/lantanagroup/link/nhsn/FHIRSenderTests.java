package com.lantanagroup.link.nhsn;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.ResourceIdChanger;
import com.lantanagroup.link.auth.OAuth2Helper;
import org.apache.http.client.methods.HttpPost;
import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import com.lantanagroup.link.config.sender.FHIRSenderConfig;

import javax.servlet.http.HttpServletRequest;

public class FHIRSenderTests {

    @Autowired
    private FHIRSenderConfig config;

    @Test
    public void sendTest() throws Exception{
        Assume.assumeTrue(config.getOAuthConfig() != null);
        Assume.assumeTrue(config.getOAuthConfig().getClientId() != null);
        Assume.assumeTrue(config.getOAuthConfig().getScope() != null);
        Assume.assumeTrue(config.getOAuthConfig().getUsername() != null);
        Assume.assumeTrue(config.getOAuthConfig().getPassword() != null);
        Assume.assumeTrue(config.getOAuthConfig().getTokenUrl() != null);

        IGenericClient fhirStoreClient = Mockito.mock(IGenericClient.class);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Authentication authentication = Mockito.mock(Authentication.class);
        FhirContext context = new FhirContext();
        MeasureReport measureReport = new MeasureReport();

        FHIRSender sender = new FHIRSender();
        sender.send(measureReport, context, request, authentication, fhirStoreClient);

        //TODO: handle mock send request with appropriate resposnes for a successful send and an unauthorized send



    }


}
