package com.lantanagroup.link.query.uscore;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.*;
import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.config.query.USCoreConfig;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
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
  private ApplicationContext context;

  @Autowired
  private QueryConfig queryConfig;

  @Autowired
  private USCoreConfig usCoreConfig;

  @Autowired
  protected FhirDataProvider fhirDataProvider;

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

    Stopwatch stopwatch = this.stopwatchManager.start("query-resources-patient");

    try {
      PatientData patientData = new PatientData(this.stopwatchManager, this.otherResources, this.eventService, this.getFhirQueryServer(), criteria, context, patient, this.usCoreConfig, resourceTypes);
      patientData.loadData(measureIds);
      return patientData;
    } catch (Exception e) {
      logger.error("Error loading data for Patient with logical ID " + patient.getIdElement().getIdPart(), e);
    } finally {
      stopwatch.stop();
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
        Stopwatch stopwatch = this.stopwatchManager.start("query-patient");
        int poiIndex = patientsOfInterest.indexOf(poi);

        try {
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
            stopwatch.stop();
            return patient;
          } else if (poi.getIdentifier() != null) {
            String searchUrl = "Patient?identifier=" + poi.getIdentifier();

            logger.debug("Searching for patient at index " + poiIndex);
            // TODO: Search by identifier rather than URL (see, e.g., FhirDataProvider.findBundleByIdentifier)
            Bundle response = this.fhirQueryServer.search()
                    .byUrl(searchUrl)
                    .returnBundle(Bundle.class)
                    .execute();
            if (response.getEntry().size() != 1) {
              logger.info("Did not find one Patient with identifier " + Helper.encodeLogging(poi.getIdentifier()));
              stopwatch.stop();
              return null;
            } else {
              Patient patient = (Patient) response.getEntryFirstRep().getResource();
              patientMap.put(poi.getIdentifier(), patient);
              poi.setId(patient.getIdElement().getIdPart());
              stopwatch.stop();
              return patient;
            }
          }
        } catch (Exception e) {
          logger.error("Unable to retrieve patient with identifier " + Helper.encodeLogging(poi.toString()), e);
        }

        stopwatch.stop();
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

      // TODO - ALM 12May2023 - looking to store a Bundle which is a bunch of Patient Bundles so that getReportPatients works
      Bundle allPatientBundle = new Bundle();
      // needd to set id on the bundle
      for (Map.Entry<String, Patient> entry : patientMap.entrySet()) {
        Patient patient = entry.getValue();

        // assume bundle and patient are already instantiated
        Bundle.BundleEntryComponent bundleEntry = new Bundle.BundleEntryComponent();
        bundleEntry.setResource(patient);
        allPatientBundle.addEntry(bundleEntry);
      }

      //String allPatientBundleId = String.format("%s-%s", reportId, ReportIdHelper.);
      allPatientBundle.setId(reportId);
      allPatientBundle.setType(Bundle.BundleType.COLLECTION);
      // need to see if this already exists??? If not call createResource???
      this.fhirDataProvider.updateResource(allPatientBundle);

      // loop through the patient ids to retrieve the patientData using each patient.
      List<Patient> patients = new ArrayList<>(patientMap.values());
      logger.info(String.format("Throttling patient query load to " + threshold + " at a time"));

      patientDataFork.submit(() -> patients.parallelStream().map(patient -> {
        logger.debug(String.format("Beginning to load data for patient with logical ID %s", patient.getIdElement().getIdPart()));

        PatientData patientData = null;
        Stopwatch stopwatch = this.stopwatchManager.start("query-resources");

        try {
          patientData = this.loadPatientData(criteria, context, patient, reportId, resourceTypes, measureIds);
        } catch (Exception ex) {
          logger.error("Error loading patient data for patient {}: {}", patient.getId(), ex.getMessage(), ex);
          return null;
        } finally {
          stopwatch.stop();
        }

        Bundle patientBundle = patientData.getBundle();

        // store the data
        try {
          eventService.triggerDataEvent(EventTypes.AfterPatientDataQuery, patientBundle, criteria, context, null);

          patientBundle.setType(Bundle.BundleType.BATCH);

          patientBundle.getEntry().forEach(entry ->
                  entry.getRequest()
                          .setMethod(Bundle.HTTPVerb.PUT)
                          .setUrl(entry.getResource().getResourceType().toString() + "/" + entry.getResource().getIdElement().getIdPart())
          );

          // Make sure the patient bundle returned by query component has an ID in the correct format
          patientBundle.setId(ReportIdHelper.getPatientDataBundleId(reportId, patient.getIdElement().getIdPart()));

          // Tag the bundle as patient-data to be able to quickly look up any data that is related to a patient
          patientBundle.getMeta().addTag(Constants.MainSystem, "patient-data", null);


          eventService.triggerDataEvent(EventTypes.BeforePatientDataStore, patientBundle, criteria, context, null);

          logger.info("Storing patient data bundle Bundle/" + patientBundle.getId());

          // store data
          stopwatch = this.stopwatchManager.start("store-patient-data");
          this.fhirDataProvider.updateResource(patientBundle);
          stopwatch.stop();

          eventService.triggerDataEvent(EventTypes.AfterPatientDataStore, patientBundle, criteria, context, null);
          logger.debug("After patient data");
        } catch (Exception ex) {
          logger.info("Exception is: " + ex.getMessage());
        }

        return patient.getId();
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
