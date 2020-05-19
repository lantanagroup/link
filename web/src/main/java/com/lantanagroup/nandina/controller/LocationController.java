package com.lantanagroup.nandina.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.Config;
import com.lantanagroup.nandina.Helper;
import com.lantanagroup.nandina.hapi.HapiFhirAuthenticationInterceptor;
import com.lantanagroup.nandina.model.LocationResponse;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

@RestController
public class LocationController {
    private static final Logger logger = LoggerFactory.getLogger(LocationController.class);
    FhirContext ctx = FhirContext.forR4();
    IGenericClient fhirClient;

    public LocationController() {
        this.fhirClient = this.ctx.newRestfulGenericClient(Config.getInstance().getFhirServerBase());
        this.fhirClient.registerInterceptor(new HapiFhirAuthenticationInterceptor());
    }

    @GetMapping("api/location")
    public List<LocationResponse> getLocations(@RequestParam(required = false) String search, @RequestParam(required = false) String identifier) throws UnsupportedEncodingException {
        String url = "Location?_summary=true&_count=10";

        if (search != null && !search.isEmpty()) {
            url += "&name:contains=" + Helper.URLEncode(search);
        }

        if (identifier != null && !identifier.isEmpty()) {
            url += "&identifier=" + Helper.URLEncode(identifier);
        }

        logger.debug(String.format("Searching for locations with URL %s", url));

        List<LocationResponse> response = new ArrayList();
        Bundle locationsBundle = this.fhirClient.search()
                .byUrl(url)
                .returnBundle(Bundle.class)
                .execute();

        logger.debug(String.format("Done searching locations. Found %s locations.", locationsBundle.getTotal()));

        for (Bundle.BundleEntryComponent entry : locationsBundle.getEntry()) {
            LocationResponse newLocResponse = new LocationResponse();
            Location loc = (Location) entry.getResource();
            String name = loc.getName();

            if (name == null || name.isEmpty()) name = "Unspecified Name";

            newLocResponse.setId(loc.getIdElement().getIdPart());
            newLocResponse.setDisplay(name);

            if (loc.getIdentifier().size() > 0) {
                String resIdentifier = null;

                if (loc.getIdentifier().get(0).hasSystem() && loc.getIdentifier().get(0).hasValue()) {
                    resIdentifier = String.format("%s (%s)",
                            loc.getIdentifier().get(0).getSystem(),
                            loc.getIdentifier().get(0).getValue());
                } else if (loc.getIdentifier().get(0).hasValue()) {
                    resIdentifier = loc.getIdentifier().get(0).getValue();
                }

                newLocResponse.setIdentifier(resIdentifier);
            }

            response.add(newLocResponse);
        }

        return response;
    }
}
