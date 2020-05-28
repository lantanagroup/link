package com.lantanagroup.nandina.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.Config;
import com.lantanagroup.nandina.hapi.HapiFhirAuthenticationInterceptor;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;

public class BaseController {
    protected FhirContext ctx = FhirContext.forR4();

    protected IGenericClient getFhirClient(Authentication authentication, HttpServletRequest request) throws Exception {
        String fhirBase = Config.getInstance().getFhirServerBase();
        if (Config.getInstance().getRequireHttps() && !fhirBase.contains("https")) {
            throw new Exception("https is required for FhirClient");
        }
        String token = null;

        if (request.getHeader("fhirBase") != null) {
            fhirBase = request.getHeader("fhirBase");

            // If fhirBase is passed in header, this request is coming from Smart-on-FHIR launch context and we should
            // pass the token of the user forward to the FHIR server, rather than use what's in the config.
            token = (String) authentication.getPrincipal();
        }

        IGenericClient fhirClient = this.ctx.newRestfulGenericClient(fhirBase);
        fhirClient.registerInterceptor(new HapiFhirAuthenticationInterceptor(token));
        return fhirClient;
    }
}
