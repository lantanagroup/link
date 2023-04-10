package com.lantanagroup.link.query.uscore;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.EventService;
import com.lantanagroup.link.EventTypes;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.Helper;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import com.lantanagroup.link.nhsn.ApplyConceptMaps;
import com.lantanagroup.link.query.auth.HapiFhirAuthenticationInterceptor;
import com.lantanagroup.link.time.Stopwatch;
import com.lantanagroup.link.time.StopwatchManager;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

@Component
public class PatientScoop {
  protected static final Logger logger = LoggerFactory.getLogger(PatientScoop.class);

  @Setter
  @Autowired
  private EventService eventService;

  @Autowired
  @Getter
  @Setter
  private StopwatchManager stopwatchManager;

  @Autowired
  private ApplicationContext applicationContext;

  private HashMap<String, Resource> otherResources = new HashMap<>();

  @Setter
  private Boolean shouldPersist = true;

  private TenantService tenantService;

  @Setter
  private IGenericClient fhirQueryServer;

  public void setTenantService(TenantService tenantService) {
    this.tenantService = tenantService;

    if (tenantService.getConfig().getFhirQuery() == null) {
      logger.error("Tenant is not configured for querying using USCore query method.");
      return;
    }

    if (tenantService.getConfig().getFhirQuery().getFhirServerBase() == null) {
      logger.error("Tenant is not configured with a FHIR server base");
      return;
    }

    //this.getFhirContext().getRestfulClientFactory().setSocketTimeout(30 * 1000);   // 30 seconds
    IGenericClient client = FhirContextProvider.getFhirContext().newRestfulGenericClient(
            tenantService.getConfig().getFhirQuery().getFhirServerBase());

    /*
    LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
    loggingInterceptor.setLogRequestSummary(true);
    loggingInterceptor.setLogRequestBody(true);
    fhirQueryClient.registerInterceptor(loggingInterceptor);
     */

    if (StringUtils.isNotEmpty(tenantService.getConfig().getFhirQuery().getAuthClass())) {
      logger.debug(String.format("Authenticating queries using %s", tenantService.getConfig().getFhirQuery().getAuthClass()));

      try {
        client.registerInterceptor(new HapiFhirAuthenticationInterceptor(tenantService.getConfig().getFhirQuery(), this.applicationContext));
      } catch (ClassNotFoundException e) {
        logger.error("Error registering authentication interceptor", e);
      }
    } else {
      logger.warn("No authentication is configured for the FHIR server being queried");
    }

    this.fhirQueryServer = client;
  }

  public void execute(ReportCriteria criteria, ReportContext context, List<PatientOfInterestModel> pois, List<String> resourceTypes, List<String> measureIds) throws Exception {
    if (this.fhirQueryServer == null) {
      throw new Exception("No FHIR server to query");
    }

    this.loadPatientData(criteria, context, pois, resourceTypes, measureIds);
  }

  private synchronized PatientData loadPatientData(ReportCriteria criteria, ReportContext context, Patient patient, List<String> resourceTypes, List<String> measureIds) {
    if (patient == null) return null;

    try {
      PatientData patientData = new PatientData(this.tenantService, this.stopwatchManager, this.otherResources, this.eventService, this.fhirQueryServer, criteria, context, patient, this.tenantService.getConfig().getFhirQuery(), resourceTypes);
      patientData.loadData(measureIds);
      return patientData;
    } catch (Exception e) {
      logger.error("Error loading data for Patient with logical ID " + patient.getIdElement().getIdPart(), e);
    }

    return null;
  }

  public void loadPatientData(ReportCriteria criteria, ReportContext context, List<PatientOfInterestModel> patientsOfInterest, List<String> resourceTypes, List<String> measureIds) {
    // first get the patients and store them in the patientMap
    Map<String, Patient> patientMap = new HashMap<>();
    int threshold = this.tenantService.getConfig().getFhirQuery().getParallelPatients();
    ForkJoinPool patientDataFork = new ForkJoinPool(threshold);
    ForkJoinPool patientFork = new ForkJoinPool(threshold);

    try {
      patientFork.submit(() -> patientsOfInterest.parallelStream().map(poi -> {
        int poiIndex = patientsOfInterest.indexOf(poi);

        //noinspection unused
        try (Stopwatch stopwatch = this.stopwatchManager.start("query-patient")) {
          if (poi.getReference() != null) {
            String id = poi.getReference();

            if (id.indexOf("/") > 0) {
              id = id.substring(id.indexOf("/") + 1);
            }

            logger.debug("Retrieving patient at index " + poiIndex);
            Patient patient = this.fhirQueryServer.read()
                    .resource(Patient.class)
                    .withId(id)
                    .execute();
            patientMap.put(poi.getReference(), patient);
            poi.setId(patient.getIdElement().getIdPart());
            return patient;
          } else if (poi.getIdentifier() != null) {
            String searchUrl = "Patient?identifier=" + poi.getIdentifier();

            logger.debug("Searching for patient at index " + poiIndex);
            // TODO: Search by identifier rather than URL (see, e.g., FhirDataProvider.findBundleByIdentifier)
            Bundle response = this.fhirQueryServer.search()
                    .byUrl(searchUrl)
                    .returnBundle(Bundle.class)
                    .execute();

            if (response.getEntry().size() > 1) {
              logger.error("Found {} (more than one) Patient with identifier {}", response.getEntry().size(), Helper.encodeLogging(poi.getIdentifier()));
            } else if (response.getEntry().size() == 0) {
              logger.error("Did not find any Patient with identifier {}", Helper.encodeLogging(poi.getIdentifier()));
              return null;
            }

            if (response.getEntry().size() > 0) {
              Patient patient = (Patient) response.getEntryFirstRep().getResource();
              patientMap.put(poi.getIdentifier(), patient);
              poi.setId(patient.getIdElement().getIdPart());
              return patient;
            }
          }
        } catch (Exception e) {
          logger.error("Unable to retrieve patient with identifier " + Helper.encodeLogging(poi.toString()), e);
        }

        return null;
      }).collect(Collectors.toList())).get();
    } catch (Exception e) {
      logger.error("Error retrieving Patient resources: {}", e.getMessage(), e);
      return;
    } finally {
      if (patientFork != null) {
        patientFork.shutdown();
      }
    }

    try {
      // loop through the patient ids to retrieve the patientData using each patient.
      List<Patient> patients = new ArrayList<>(patientMap.values());
      logger.info(String.format("Throttling patient query load to " + threshold + " at a time"));

      patientDataFork.submit(() -> patients.parallelStream().map(patient -> {
        logger.debug(String.format("Beginning to load data for patient with logical ID %s", patient.getIdElement().getIdPart()));

        PatientData patientData = null;

        //noinspection unused
        try (Stopwatch stopwatch = this.stopwatchManager.start("query-resources")) {
          patientData = this.loadPatientData(criteria, context, patient, resourceTypes, measureIds);
        } catch (Exception ex) {
          logger.error("Error loading patient data for patient {}: {}", patient.getId(), ex.getMessage(), ex);
          return null;
        }

        Bundle patientBundle = patientData.getBundle();

        // store the data
        try {
          ApplyConceptMaps applyConceptMaps = new ApplyConceptMaps();
          applyConceptMaps.execute(tenantService, patientBundle);

          eventService.triggerDataEvent(this.tenantService, EventTypes.AfterPatientDataQuery, patientBundle, criteria, context, null);

          List<com.lantanagroup.link.db.model.PatientData> dbPatientData = patientData.getBundle().getEntry().stream().map(entry -> {
            com.lantanagroup.link.db.model.PatientData dbpd = new com.lantanagroup.link.db.model.PatientData();
            dbpd.setPatientId(patient.getIdElement().getIdPart());
            dbpd.setResourceType(entry.getResource().getResourceType().toString());
            dbpd.setResourceId(entry.getResource().getIdElement().getIdPart());
            dbpd.setResource(entry.getResource());
            return dbpd;
          }).collect(Collectors.toList());

          eventService.triggerDataEvent(this.tenantService, EventTypes.BeforePatientDataStore, patientBundle, criteria, context, null);

          if (this.shouldPersist) {
            logger.info("Storing patient data for patient {}", patient.getIdElement().getIdPart());

            // store data
            //noinspection unused
            try (Stopwatch stopwatch = this.stopwatchManager.start("store-patient-data")) {
              this.tenantService.savePatientData(dbPatientData);
            }
          } else {
            logger.info("Not storing patient data bundle");
          }

          eventService.triggerDataEvent(this.tenantService, EventTypes.AfterPatientDataStore, patientBundle, criteria, context, null);
          logger.debug("After patient data");
        } catch (Exception ex) {
          logger.error("Exception is: " + ex.getMessage());
        }

        return patient.getIdElement().getIdPart();
      }).collect(Collectors.toList())).get();
    } catch (Exception e) {
      logger.error("Error scooping data for patients {}", e.getMessage(), e);
    } finally {
      if (patientDataFork != null) {
        patientDataFork.shutdown();
      }
    }
  }
}
