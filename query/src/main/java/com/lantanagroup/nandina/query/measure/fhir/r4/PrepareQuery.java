package com.lantanagroup.nandina.query.measure.fhir.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.util.BundleUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lantanagroup.nandina.MeasureConfig;
import com.lantanagroup.nandina.query.BasePrepareQuery;
import com.lantanagroup.nandina.query.scoop.PatientScoop;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ResourceType;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PrepareQuery extends BasePrepareQuery {
    private FhirContext ctx = null;
    private IGenericClient targetFhirServer;
    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public void execute() throws Exception {
        String measureId = this.getCriteria().get("measureId");
        String measureConfigUrl = null;
        String reportDate = this.getCriteria().get("reportDate");
        List<String> patientIds;
        IGenericClient fhirQueryClient = (IGenericClient) this.getContextData("fhirQueryClient");
        ctx = fhirQueryClient.getFhirContext();
        targetFhirServer = ctx.newRestfulGenericClient(this.properties.getFhirServerQueryBase());
        Bundle measureBundle = null;

        List<MeasureConfig> measureConfigs = mapper.convertValue(this.properties.getMeasureConfigs(), new TypeReference<List<MeasureConfig>>() { });

        for (MeasureConfig measureConfig : measureConfigs) {
            if (measureConfig.getId().equals(measureId)) {
                measureConfigUrl = measureConfig.getUrl();
            }
        }

        if (null != measureConfigUrl) {
            HttpClient client = HttpClient.newHttpClient();
            log.info("Calling <GET> Request <" + measureConfigUrl + ">");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(measureConfigUrl))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Response statusCode: " + response.statusCode());

            IParser parser = ctx.newJsonParser();
            try {
                measureBundle = parser.parseResource(Bundle.class, response.body());
            } catch (Exception ex) {
                log.error("Error trying to parse the response into a bundle from this url: " + measureConfigUrl);
            }

            // store the latest measure onto the cqf-ruler server
            log.info("Calling storeLatestMeasure()");
            storeLatestMeasure(measureBundle, fhirQueryClient);
            // retrieve patient ids from the bundles
            patientIds = retrievePatientIds(reportDate);

            // scoop the patient data based on the list of patient ids
            PatientScoop patientScoop = new PatientScoop(targetFhirServer, this.getFhirClient(), patientIds);
            patientScoop.getPatientData().forEach(data -> {
                try {
                    Bundle bundle = data.getBundleTransaction();
                    IGenericClient newClient = ctx.newRestfulGenericClient(this.properties.getFhirServerStoreBase());
                    log.info("Storing scooped data on storage fhir server for PatientId: " + data.getPatient().getIdElement().getIdPart());
                    Bundle bundleResponse = newClient.transaction().withBundle(bundle).execute();
                    log.info("Successfully stored scooped data for PatientId: " + data.getPatient().getIdElement().getIdPart());
                } catch (Exception e) {
                    String message = e.getMessage() != null ? e.getMessage() : "No message provided";
                    log.error("Could not store scooped patient data for PatientId " + data.getPatient().getIdElement().getIdPart() + " due to: " + message);
                    e.printStackTrace();
                }
            });
        } else {
            log.error("measure url is null, check config!");
        }
    }

    /**
     * This method checks the CQF-Ruler server for all bundles created on the same day as the report date from the UI
     * It loads all the pages of bundles and returns them all. We then call the PatientScoop to scooop all of the
     * patientData. This is then used to find all patients by identifier.
     * @param date
     * @return patiendId's <all of the patient identifiers found in the bundles
     */
    private List<String> retrievePatientIds(String date) {
        IGenericClient cqfRulerClient = ctx.newRestfulGenericClient(this.properties.getFhirServerStoreBase());
        List<String> patientIds = new ArrayList<>();
        List<IBaseResource> bundles = new ArrayList<>();

        if (null != date) {
            try {
                Bundle bundle = cqfRulerClient
                        .search()
                        .forResource(Bundle.class)
                        .where(Bundle.TIMESTAMP.exactly().day(date))
                        .returnBundle(Bundle.class)
                        .execute();
                bundles.addAll(BundleUtil.toListOfResources(ctx, bundle));

                // Load the subsequent pages
                while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
                    bundle = cqfRulerClient
                            .loadPage()
                            .next(bundle)
                            .execute();
                    log.info("Adding next page of bundles...");
                    bundles.addAll(BundleUtil.toListOfResources(ctx, bundle));
                }

                bundles.parallelStream().forEach(bundleResource -> {
                    Bundle resource = (Bundle) ctx.newJsonParser().parseResource(ctx.newJsonParser().setPrettyPrint(false).encodeResourceToString(bundleResource));
                    resource.getEntry().parallelStream().forEach(entry -> {
                        if (entry.getResource().getResourceType().equals(ResourceType.Patient)) {
                            Patient p = (Patient) entry.getResource();
                            if (null != p.getIdentifier().get(0)) {
                                String patientId =
                                        p.getIdentifier().get(0).getSystem() +
                                                "|" +
                                                p.getIdentifier().get(0).getValue();
                                patientIds.add(patientId);
                            }
                        }
                    });
                });
            } catch (AuthenticationException ae) {
                log.error("Unable to retrieve resource with date " + date + " from CQF Ruler server " + this.properties.getFhirServerStoreBase() + " due to authentication errors: \n" + ae.getResponseBody());
                ae.printStackTrace();
            } catch (Exception ex) {
                log.error("Could not retrieve data from CQF Ruler server " + this.properties.getFhirServerStoreBase() + " for " + this.getClass().getName() + ": " + ex.getMessage(), ex);
            }
        } else {
            log.error("Report date is null!");
        }
        log.info("Loaded " + patientIds.size() + " patient ids");
        patientIds.forEach(id -> log.info("PatientId: " + id));
        return patientIds;
    }

    private void storeLatestMeasure(Bundle bundle, IGenericClient fhirQueryClient) {
        log.info("Generating a Bundle Transaction of the Measure");
        bundle.setType(Bundle.BundleType.TRANSACTION);
        IGenericClient client = ctx.newRestfulGenericClient(this.properties.getFhirServerStoreBase());
        log.info("Executing the Bundle");
        Bundle resp = client.transaction().withBundle(bundle).execute();
        log.info("Bundle executed successfully...");
        String measureId = StringUtils.substringBetween(resp.getEntry().get(0).getResponse().getLocation(), "Measure/", "/_history");
        log.info("Storing measureId " + measureId + " in context");
        this.addContextData("measureId", measureId );
        this.addContextData("fhirContext", fhirQueryClient.getFhirContext());
    }
}