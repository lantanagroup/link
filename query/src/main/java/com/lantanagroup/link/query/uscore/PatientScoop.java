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
import java.util.concurrent.*;

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

  private synchronized PatientData loadPatientData(ReportCriteria criteria, ReportContext context, Patient patient, List<String> resourceTypes, List<String> measureIds) {
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

  public List<Patient> queryAndGetPatients(List<PatientOfInterestModel> patientsOfInterest) {
    List<Patient> patients = new ArrayList<>();
    int threshold = usCoreConfig.getParallelPatients();
    ForkJoinPool patientFork = new ForkJoinPool(threshold);

    for(PatientOfInterestModel poi : patientsOfInterest) {
      patientFork.submit(
              () -> {
                Stopwatch stopwatch = this.stopwatchManager.start("query-patient");
                int poiIndex = patientsOfInterest.indexOf(poi);

                try {
                  if (poi.getReference() != null) {
                    String id = poi.getReference();

                    if (id.indexOf("/") > 0) {
                      id = id.substring(id.indexOf("/") + 1);
                    }

                    logger.debug("Retrieving patient at index " + poiIndex);
                    Patient patient;
                    try {
                      patient = this.fhirQueryServer.read()
                              .resource(Patient.class)
                              .withId(id)
                              .execute();
                    } catch (Exception ex) {
                      patient = new Patient();
                      patient.setId(String.format("Patient/%s",id));
                    }
                    patients.add(patient);
                    poi.setId(patient.getIdElement().getIdPart());
                    stopwatch.stop();
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
                    } else {
                      Patient patient = (Patient) response.getEntryFirstRep().getResource();
                      patients.add(patient);
                      poi.setId(patient.getIdElement().getIdPart());
                      stopwatch.stop();
                    }
                  }
                } catch (Exception e) {
                  logger.error("Unable to retrieve patient with identifier " + Helper.encodeLogging(poi.toString()), e);
                }
              }
      );
    }

    patientFork.shutdown();

    try {
      while (!patientFork.awaitTermination(30, TimeUnit.SECONDS)) {
        logger.info("Waiting for patient queries to complete...");
      }
      logger.info("All patient queries completed.");
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    return patients;
  }

  public Bundle getPatientBundle(Patient patient, ReportCriteria criteria, ReportContext context, String reportId, List<String> resourceTypes, List<String> measureIds) {

    PatientData patientData;
    Stopwatch stopwatch = this.stopwatchManager.start("query-resources");

    try {
      patientData = this.loadPatientData(criteria, context, patient, resourceTypes, measureIds);
    } catch (Exception ex) {
      logger.error("Error loading patient data for patient {}: {}", patient.getId(), ex.getMessage(), ex);
      return null;
    } finally {
      stopwatch.stop();
    }

    Bundle patientBundle = patientData.getBundle();

    patientBundle.setType(Bundle.BundleType.BATCH);

    patientBundle.getEntry().forEach(entry ->
            entry.getRequest()
                    .setMethod(Bundle.HTTPVerb.PUT)
                    .setUrl(entry.getResource().getResourceType().toString() + "/" + entry.getResource().getIdElement().getIdPart())
    );

    // Make sure the patient bundle returned by query component has an ID in the correct format
    patientBundle.setId(ReportIdHelper.getPatientDataBundleId(reportId, patient.getIdElement().getIdPart()));

    // Tag the bundle as patient-data to be able to quickly look up any data that is related to a patient
    patientBundle.getMeta().addTag(Constants.MainSystem, Constants.patientDataTag, "Patient Data");

    return patientBundle;
  }

  public void queryAndGetPatientData(ReportCriteria criteria, ReportContext context, String reportId, List<String> resourceTypes, List<String> measureIds, List<Patient> patients) {
    int threshold = usCoreConfig.getParallelPatients();
    ForkJoinPool patientDataFork = new ForkJoinPool(threshold);

    for(Patient patient : patients) {
      patientDataFork.submit(
              () -> {
                // Get & Store patient data as Bundle
                Bundle patientBundle = null;
                try {
                  logger.debug("START : Getting Patient Bundle For Patient ID '{}'", patient.getIdElement().getIdPart());
                  patientBundle = getPatientBundle(patient, criteria, context, reportId, resourceTypes, measureIds);
                  logger.debug("END : Getting Patient Bundle For Patient ID '{}'", patient.getIdElement().getIdPart());
                } catch (Exception ex) {
                  logger.error("Issue Getting Patient Bundle For Patient ID '{}' - Message {}",
                          patient.getIdElement().getIdPart(),
                          ex.getMessage());
                }

                try {
                  if (patientBundle != null) {
                    eventService.triggerDataEvent(EventTypes.AfterPatientDataQuery, patientBundle, criteria, context, null);
                    eventService.triggerDataEvent(EventTypes.BeforePatientDataStore, patientBundle, criteria, context, null);

                    logger.info("START: Storing Patient data bundle Bundle/" + patientBundle.getId());

                    Stopwatch stopwatch = this.stopwatchManager.start("store-patient-data");
                    this.fhirDataProvider.updateResource(patientBundle);
                    stopwatch.stop();

                    eventService.triggerDataEvent(EventTypes.AfterPatientDataStore, patientBundle, criteria, context, null);

                    logger.debug("END: Storing Patient data bundle Bundle/" + patientBundle.getId());
                  }
                } catch (Exception ex) {
                  logger.info("Error getting/storing Patient data bundle - Exception is: " + ex.getMessage());
                }
              }
      );
    }

    patientDataFork.shutdown();

    try {
      while (!patientDataFork.awaitTermination(30, TimeUnit.SECONDS)) {
        logger.info("Waiting for patient data queries to complete...");
      }
      logger.info("All patient data queries completed.");
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

  }

  public void loadPatientData(ReportCriteria criteria, ReportContext context, List<PatientOfInterestModel> patientsOfInterest, String reportId, List<String> resourceTypes, List<String> measureIds) {
    int threshold = usCoreConfig.getParallelPatients();

    List<Patient> patients = null;
    try {
    // First get all patients and store them in List
    patients = queryAndGetPatients(patientsOfInterest);
    } catch (Exception e) {
      logger.error("Error scooping data for patients {}", e.getMessage(), e);
    }

    try {
      // loop through the patient ids to retrieve the patientData using each patient.
      logger.info(String.format("Throttling patient query load to " + threshold + " at a time"));

      queryAndGetPatientData(criteria,context,reportId,resourceTypes,measureIds,patients);

    } catch (Exception e) {
      logger.error("Error scooping data for patients {}", e.getMessage(), e);
    }
  }
}
