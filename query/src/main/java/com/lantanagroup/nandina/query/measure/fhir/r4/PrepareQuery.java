package com.lantanagroup.nandina.query.measure.fhir.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.util.BundleUtil;
import com.lantanagroup.nandina.NandinaConfig;
import com.lantanagroup.nandina.query.BasePrepareQuery;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.scoop.PatientScoop;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PrepareQuery extends BasePrepareQuery {
    private static final Logger logger = LoggerFactory.getLogger(com.lantanagroup.nandina.query.measure.fhir.r4.PrepareQuery.class);
    private FhirContext ctx = null;
    private IGenericClient targetFhirServer;
    private IGenericClient nandinaFhirServer;

    @Autowired
    NandinaConfig nandinaConfig;

    @Override
    public void execute() throws Exception {
        String measureId = this.getCriteria().get("measureId");
        String date = this.getCriteria().get("reportDate");
        List<String> patientIds = new ArrayList<>();
        IGenericClient fhirQueryClient = (IGenericClient) this.getContextData("fhirQueryClient");
        ctx = fhirQueryClient.getFhirContext();
        targetFhirServer = ctx.newRestfulGenericClient("https://fhir.nandina.org/fhir");
        nandinaFhirServer = ctx.newRestfulGenericClient("https://fhir.nandina.org/fhir");

        // retrieve the measure selected
        Measure measure = fhirQueryClient.read().resource(Measure.class).withId(measureId).execute();

        // store the latest measure onto the cqf-ruler server
        storeLatestMeasure(measure, fhirQueryClient);
        // retrieve patient ids from the bundles
        patientIds = retrievePatientIds(date);

        // scoop the patient data based on the list of patient ids
        PatientScoop patientScoop = new PatientScoop(targetFhirServer, nandinaFhirServer, patientIds);
        patientScoop.getPatientData().parallelStream().forEach(data -> {
            Bundle bundle = data.getBundleTransaction();
            IGenericClient client = ctx.newRestfulGenericClient("https://cqf-ruler.nandina.org/cqf-ruler-r4/fhir");
            Bundle bundleResponse = client.transaction().withBundle(bundle).execute();
            logger.info(ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundleResponse));
        });
    }

    private List<String> retrievePatientIds(String date) {
        IGenericClient cqfRulerClient = ctx.newRestfulGenericClient("https://cqf-ruler.nandina.org/cqf-ruler-r4/fhir");
        List<String> patientIds = new ArrayList<>();
        List<IBaseResource> bundles = new ArrayList<>();

        if (null != date) {
            Bundle bundle = cqfRulerClient
                    .search()
                    .forResource(Bundle.class)
                    .lastUpdated(new DateRangeParam(date, LocalDate.parse(date).plusDays(1).toString()))
                    .returnBundle(Bundle.class)
                    .execute();
            bundles.addAll(BundleUtil.toListOfResources(ctx, bundle));

            // Load the subsequent pages
            while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
                bundle = cqfRulerClient
                        .loadPage()
                        .next(bundle)
                        .execute();
                logger.info("Adding next page of bundles...");
                bundles.addAll(BundleUtil.toListOfResources(ctx, bundle));
            }

            bundles.parallelStream().forEach(bundleResource -> {
                Bundle resource = (Bundle) ctx.newJsonParser().parseResource(ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundleResource));
                resource.getEntry().parallelStream().forEach(entry -> {
                    if (entry.getResource().getResourceType().equals(ResourceType.Patient)) {
                        patientIds.add(entry.getResource().getIdElement().getIdPart());
                    }
                });
            });
        } else {
            logger.error("Report date is null!");
        }
        logger.info("Loaded " + patientIds.size() + " patient ids");
        return patientIds;
    }

    private void storeLatestMeasure(Measure measure, IGenericClient fhirQueryClient) {
        Bundle bundle = getBundleTransaction(measure);
        IGenericClient client = ctx.newRestfulGenericClient("https://cqf-ruler.nandina.org/cqf-ruler-r4/fhir/");
        Bundle resp = client.transaction().withBundle(bundle).execute();
        logger.info("Logging measureId");
        this.addContextData("measureId", StringUtils.substringBetween(resp.getEntry().get(0).getResponse().getLocation(), "Measure/", "/_history"));
        this.addContextData("fhirContext", fhirQueryClient.getFhirContext());
    }

    private Bundle getBundleTransaction(Measure measure) {
        Bundle b = new Bundle();
        b.setType(Bundle.BundleType.TRANSACTION);
        b.addEntry().setResource(measure).getRequest().setMethod(Bundle.HTTPVerb.POST);
        return b;
    }
}

