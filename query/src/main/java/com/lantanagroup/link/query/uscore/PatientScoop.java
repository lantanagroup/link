package com.lantanagroup.link.query.uscore;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.EventService;
import com.lantanagroup.link.EventTypes;
import com.lantanagroup.link.Helper;
import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.config.query.USCoreConfig;
import com.lantanagroup.link.db.MongoService;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import com.lantanagroup.link.time.Stopwatch;
import com.lantanagroup.link.time.StopwatchManager;
import lombok.Getter;
import lombok.Setter;
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

@Getter
@Setter
@Component
public class PatientScoop {
  protected static final Logger logger = LoggerFactory.getLogger(PatientScoop.class);

  protected IGenericClient fhirQueryServer;

  @Autowired
  private MongoService mongoService;

  @Autowired
  private ApplicationContext context;

  @Autowired
  private QueryConfig queryConfig;

  @Autowired
  private USCoreConfig usCoreConfig;

  @Setter
  @Autowired
  private EventService eventService;

  @Autowired
  private StopwatchManager stopwatchManager;

  private HashMap<String, Resource> otherResources = new HashMap<>();

  public void execute(ReportCriteria criteria, ReportContext context, List<PatientOfInterestModel> pois, String reportId, List<String> resourceTypes, List<String> measureIds) throws Exception {
    if (this.fhirQueryServer == null) {
      throw new Exception("No FHIR server to query");
    }

    this.loadPatientData(criteria, context, pois, reportId, resourceTypes, measureIds);
  }

  private synchronized PatientData loadPatientData(ReportCriteria criteria, ReportContext context, Patient patient, String reportId, List<String> resourceTypes, List<String> measureIds) {
    if (patient == null) return null;

    try {
      PatientData patientData = new PatientData(this.stopwatchManager, this.otherResources, this.eventService, this.getFhirQueryServer(), criteria, context, patient, this.usCoreConfig, resourceTypes);
      patientData.loadData(measureIds);
      return patientData;
    } catch (Exception e) {
      logger.error("Error loading data for Patient with logical ID " + patient.getIdElement().getIdPart(), e);
    }

    return null;
  }

  public void loadPatientData(ReportCriteria criteria, ReportContext context, List<PatientOfInterestModel> patientsOfInterest, String reportId, List<String> resourceTypes, List<String> measureIds) {
    // first get the patients and store them in the patientMap
    Map<String, Patient> patientMap = new HashMap<>();
    int threshold = usCoreConfig.getParallelPatients();
    ForkJoinPool patientDataFork = new ForkJoinPool(threshold);
    ForkJoinPool patientFork = new ForkJoinPool(threshold);

    try {
      patientFork.submit(() -> patientsOfInterest.parallelStream().map(poi -> {
        int poiIndex = patientsOfInterest.indexOf(poi);

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

        try (Stopwatch stopwatch = this.stopwatchManager.start("query-resources")) {
          patientData = this.loadPatientData(criteria, context, patient, reportId, resourceTypes, measureIds);
        } catch (Exception ex) {
          logger.error("Error loading patient data for patient {}: {}", patient.getId(), ex.getMessage(), ex);
          return null;
        }

        Bundle patientBundle = patientData.getBundle();

        // store the data
        try {
          List<com.lantanagroup.link.db.model.PatientData> dbPatientData = patientData.getBundle().getEntry().stream().map(entry -> {
            com.lantanagroup.link.db.model.PatientData dbpd = new com.lantanagroup.link.db.model.PatientData();
            dbpd.setPatientId(patient.getIdElement().getIdPart());
            dbpd.setResourceType(entry.getResource().getResourceType().toString());
            dbpd.setResourceId(entry.getResource().getIdElement().getIdPart());
            dbpd.setResource(entry.getResource());
            return dbpd;
          }).collect(Collectors.toList());

          eventService.triggerDataEvent(EventTypes.BeforePatientDataStore, patientBundle, criteria, context, null);

          logger.info("Storing patient data bundle Bundle/" + patientBundle.getId());

          // store data
          try (Stopwatch stopwatch = this.stopwatchManager.start("store-patient-data")) {
            this.mongoService.savePatientData(dbPatientData);
          }

          eventService.triggerDataEvent(EventTypes.AfterPatientDataStore, patientBundle, criteria, context, null);
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
