package com.lantanagroup.nandina.query.measure.fhir.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.BundleUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lantanagroup.nandina.query.BasePrepareQuery;
import com.lantanagroup.nandina.query.scoop.PatientScoop;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ResourceType;

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
        String reportDate = this.getCriteria().get("reportDate");
        List<String> patientIds;
        this.targetFhirServer = (IGenericClient) this.getContextData("fhirStoreClient");
        this.ctx = (FhirContext) this.getContextData("fhirContext");

        if (StringUtils.isNotEmpty(measureId)) {
            // retrieve patient ids from the bundles
            patientIds = retrievePatientIds(reportDate);

            // scoop the patient data based on the list of patient ids
            PatientScoop patientScoop = new PatientScoop(targetFhirServer, this.getFhirClient(), patientIds);
            patientScoop.getPatientData().forEach(data -> {
                try {
                    Bundle bundle = data.getBundleTransaction();
                    log.info("Storing scooped data on storage fhir server for PatientId: " + data.getPatient().getIdElement().getIdPart());

                    this.targetFhirServer.transaction().withBundle(bundle).execute();
                    log.info("Successfully stored scooped data for PatientId: " + data.getPatient().getIdElement().getIdPart());
                } catch (Exception e) {
                    String message = e.getMessage() != null ? e.getMessage() : "No message provided";
                    log.error("Could not store scooped patient data for PatientId " + data.getPatient().getIdElement().getIdPart() + " due to: " + message);
                    e.printStackTrace();
                }
            });
        } else {
            log.error("measure id is null, can't continue!");
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
            Bundle bundle = cqfRulerClient
                    .search()
                    .forResource(Bundle.class)
                    .where(Bundle.TIMESTAMP.exactly().day(date))
                    .returnBundle(Bundle.class)
                    .execute();

            if (bundle == null) {
                return patientIds;
            }

            if (bundle.getEntry().size() == 0) {
                log.info("No RR bundles found matching time stamp " + date);
                return patientIds;
            }

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
        } else {
            log.error("Report date is null!");
        }
        log.info("Loaded " + patientIds.size() + " patient ids");
        patientIds.forEach(id -> log.info("PatientId: " + id));
        return patientIds;
    }
}