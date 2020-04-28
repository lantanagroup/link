package com.lantanagroup.nandina.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.Config;
import com.lantanagroup.nandina.hapi.HapiFhirAuthenticationInterceptor;
import com.lantanagroup.nandina.model.LocationResponse;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Location;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

@RestController
public class LocationController {
    FhirContext ctx = FhirContext.forR4();
    IGenericClient fhirClient;

    public LocationController() {
        this.fhirClient = this.ctx.newRestfulGenericClient(Config.getInstance().getFhirServerBase());
        this.fhirClient.registerInterceptor(new HapiFhirAuthenticationInterceptor());
    }

    @GetMapping("api/location")
    public List<LocationResponse> getLocations(@RequestParam(required = false) String search) throws UnsupportedEncodingException {
        String url = "Location?_summary=true&_count=10";

        if (search != null && !search.isEmpty()) {
            url += "&name:contains=" + URLEncoder.encode(search, "utf-8");
        }

        List<LocationResponse> response = new ArrayList();
        Bundle locationsBundle = this.fhirClient.search()
                .byUrl(url)
                .returnBundle(Bundle.class)
                .execute();

        for (Bundle.BundleEntryComponent entry : locationsBundle.getEntry()) {
            LocationResponse newLocResponse = new LocationResponse();
            Location loc = (Location) entry.getResource();
            String name = loc.getName();

            if (name == null || name.isEmpty()) name = "Unspecified Name";

            newLocResponse.setId(loc.getIdElement().getIdPart());
            newLocResponse.setDisplay(name);
            response.add(newLocResponse);
        }

        return response;
    }
}
