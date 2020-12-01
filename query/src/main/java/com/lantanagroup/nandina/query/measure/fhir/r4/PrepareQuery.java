package com.lantanagroup.nandina.query.measure.fhir.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.NandinaConfig;
import com.lantanagroup.nandina.query.BasePrepareQuery;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Measure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class PrepareQuery extends BasePrepareQuery {
    private static final Logger logger = LoggerFactory.getLogger(com.lantanagroup.nandina.query.measure.fhir.r4.PrepareQuery.class);
    private FhirContext ctx = null;

    @Autowired
    NandinaConfig nandinaConfig;

    @Override
    public void execute() throws Exception {
        String measureId = this.getCriteria().get("measureId");

        IGenericClient fhirQueryClient = (IGenericClient) this.getContextData("fhirQueryClient");
        ctx = fhirQueryClient.getFhirContext();

        Measure measure = fhirQueryClient.read().resource(Measure.class).withId(measureId).execute();

        Bundle bundle = getBundleTransaction(measure);
        IGenericClient client = ctx.newRestfulGenericClient("https://cqm-sandbox.alphora.com/cqf-ruler-r4/fhir/");
        Bundle resp = client.transaction().withBundle(bundle).execute();
        logger.info("Logging measureId");
        this.addContextData("measureId", StringUtils.substringBetween(resp.getEntry().get(0).getResponse().getLocation(), "Measure/", "/_history"));
        this.addContextData("fhirContext", fhirQueryClient.getFhirContext());
    }

    public Bundle getBundleTransaction(Measure measure) {
        Bundle b = new Bundle();
        b.setType(Bundle.BundleType.TRANSACTION);
        b.addEntry().setResource(measure).getRequest().setMethod(Bundle.HTTPVerb.POST);
        return b;
    }
}

