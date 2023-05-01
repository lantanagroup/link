package com.lantanagroup.link.query.uscore.scoop;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.*;
import com.lantanagroup.link.config.api.ApiConfigEvents;
import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.config.query.USCoreConfig;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import com.lantanagroup.link.query.uscore.PatientData;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
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
public class PatientScoop extends Scoop {
  protected IGenericClient fhirQueryServer;

  @Autowired
  private ApplicationContext context;

  //@Autowired
  private QueryConfig queryConfig;

  //@Autowired
  //none of the autowired stuff seems to be working w/ this class, trying to setup
  // because we need this config
  private USCoreConfig usCoreConfig;

  //@Autowired
  protected FhirDataProvider fhirDataProvider;

  /* TODO : ALM - commenting this out and setting in added constructor until autowired snafu figured out
  @Setter
  @Autowired
   */
  private EventService eventService;

  public PatientScoop() {
    // TODO - trying to get past the dependency / autowired snafu for now.
    eventService = new EventService();

  }

  public void execute(ReportCriteria criteria, ReportContext context, List<PatientOfInterestModel> pois, String reportId, List<String> resourceTypes, List<String> measureIds) throws Exception {
    if (this.fhirQueryServer == null) {
      throw new Exception("No FHIR server to query");
    }

    this.loadPatientData(criteria, context, pois, reportId, resourceTypes, measureIds);
  }

  private synchronized PatientData loadPatientData(ReportCriteria criteria, ReportContext context, Patient patient, String reportId, List<String> resourceTypes, List<String> measureIds) {
    if (patient == null) return null;

    try {
      PatientData patientData = new PatientData(this.getFhirQueryServer(), criteria, patient, this.usCoreConfig, resourceTypes);
      patientData.loadData(measureIds);
      return patientData;
    } catch (Exception e) {
      logger.error("Error loading data for Patient with logical ID " + patient.getIdElement().getIdPart(), e);
    }

    return null;
  }

  public PatientData loadPatientData(ReportCriteria criteria, ReportContext context, List<PatientOfInterestModel> patientsOfInterest, String reportId, List<String> resourceTypes, List<String> measureIds) {
    // first get the patients and store them in the patientMap
    Map<String, Patient> patientMap = new HashMap<>();
    patientsOfInterest.forEach(poi -> {
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
          } else {
            Patient patient = (Patient) response.getEntryFirstRep().getResource();
            patientMap.put(poi.getIdentifier(), patient);
            poi.setId(patient.getIdElement().getIdPart());
          }
        }


      } catch (Exception e) {
        logger.error("Unable to retrieve patient with identifier " + Helper.encodeLogging(poi.toString()), e);
      }
    });
    ForkJoinPool forkJoinPool = null;
    try {
      // loop through the patient ids to retrieve the patientData using each patient.
      List<Patient> patients = new ArrayList<>(patientMap.values());
      int threshold = usCoreConfig.getParallelPatients();
      logger.info(String.format("Throttling patient query load to " + threshold + " at a time"));
      forkJoinPool = new ForkJoinPool(threshold);

      forkJoinPool.submit(() -> patients.parallelStream().map(patient -> {
        logger.debug(String.format("Beginning to load data for patient with logical ID %s", patient.getIdElement().getIdPart()));

        PatientData patientData = this.loadPatientData(criteria, context, patient, reportId, resourceTypes, measureIds);

        Bundle patientBundle = patientData.getBundleTransaction();
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

          // staore data
          this.fhirDataProvider.updateResource(patientBundle);

          eventService.triggerDataEvent(EventTypes.AfterPatientDataStore, patientBundle, criteria, context, null);
          logger.debug("After patient data");
        } catch (Exception ex) {
          logger.info("Exception is: " + ex.getMessage());
        }
        return patientData;
      }).collect(Collectors.toList())).get();
    } catch (Exception e) {
      logger.error(e.getMessage());
    } finally {
      if (forkJoinPool != null) {
        forkJoinPool.shutdown();
      }
    }

    // TODO: Change method return type to void
    return null;
  }
}
