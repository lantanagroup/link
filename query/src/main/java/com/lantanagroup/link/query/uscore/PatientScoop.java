package com.lantanagroup.link.query.uscore;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.*;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.DataTrace;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import com.lantanagroup.link.query.QueryPhase;
import com.lantanagroup.link.time.Stopwatch;
import com.lantanagroup.link.time.StopwatchManager;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static ca.uhn.fhir.rest.api.Constants.HEADER_REQUEST_ID;

@Getter
@Setter
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PatientScoop {
  protected static final Logger logger = LoggerFactory.getLogger(PatientScoop.class);

  protected IGenericClient fhirQueryServer;

  @Autowired
  private ApplicationContext context;

  @Setter
  @Autowired
  private EventService eventService;

  @Autowired
  private StopwatchManager stopwatchManager;

  @Setter
  private Boolean shouldPersist = true;

  @Setter
  private TenantService tenantService;

  private ApplyConceptMaps applyConceptMaps;

  public void execute(ReportCriteria criteria, ReportContext context, List<PatientOfInterestModel> pois, QueryPhase queryPhase) throws Exception {
    if (this.fhirQueryServer == null) {
      throw new Exception("No FHIR server to query");
    }

    this.applyConceptMaps = new ApplyConceptMaps();

    switch (queryPhase) {
      case INITIAL:
        this.loadInitialPatientData(criteria, context, pois);
        break;
      case SUPPLEMENTAL:
        this.loadSupplementalPatientData(criteria, context, pois);
        break;
    }
  }

  private PatientData loadInitialPatientData(ReportCriteria criteria, ReportContext context, Patient patient) {
    if (patient == null) return null;

    try {
      PatientData patientData = new PatientData(this.stopwatchManager, this.tenantService, this.eventService, this.getFhirQueryServer(), criteria, context, this.tenantService.getConfig().getFhirQuery());
      patientData.loadInitialData(patient);
      return patientData;
    } catch (ReportFailureException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Error loading data for Patient with logical ID " + patient.getIdElement().getIdPart(), e);
    }

    return null;
  }

  private PatientData loadSupplementalPatientData(ReportCriteria criteria, ReportContext context, String patientId, Bundle patientBundle) {
    if (patientBundle == null) return null;

    try {
      PatientData patientData = new PatientData(this.stopwatchManager, this.tenantService, this.eventService, this.getFhirQueryServer(), criteria, context, this.tenantService.getConfig().getFhirQuery());
      patientData.loadSupplementalData(patientId, patientBundle);
      return patientData;
    } catch (ReportFailureException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Error loading data for Patient with logical ID " + patientId, e);
    }

    return null;
  }

  private Semaphore getSemaphore() {
    return new Semaphore(this.tenantService.getConfig().getFhirQuery().getQueryThreads(), true);
  }

  public void loadInitialPatientData(ReportCriteria criteria, ReportContext context, List<PatientOfInterestModel> patientsOfInterest) {
    // first get the patients and store them in the patientMap
    Map<String, Patient> patientMap = new ConcurrentHashMap<>();
    ForkJoinPool patientDataFork = ForkJoinPool.commonPool();
    ForkJoinPool patientFork = ForkJoinPool.commonPool();
    AtomicInteger progress = new AtomicInteger(0);
    Semaphore semaphore = getSemaphore();

    try {
      var contextMapCopy = MDC.getCopyOfContextMap();
      patientFork.submit(() -> patientsOfInterest.parallelStream().map(poi -> {
        semaphore.acquireUninterruptibly();

        //noinspection unused
        try (Stopwatch stopwatch = this.stopwatchManager.start(Constants.TASK_PATIENT, Constants.CATEGORY_QUERY)) {
          MDC.setContextMap(contextMapCopy);
          int poiIndex = patientsOfInterest.indexOf(poi);
          UUID queryId = UUID.randomUUID();
          if (poi.getReference() != null) {
            String id = poi.getReference();
            if (id.indexOf("/") > 0) {
              id = id.substring(id.indexOf("/") + 1);
            }

            logger.debug("Retrieving patient at index " + poiIndex);
            Patient patient = this.fhirQueryServer.read()
                    .resource(Patient.class)
                    .withId(id)
                    .withAdditionalHeader(HEADER_REQUEST_ID, queryId.toString())
                    .execute();
            tenantService.saveDataTraces(queryId, id, List.of(patient));
            patient.getMeta().addExtension(Constants.ReceivedDateExtensionUrl, DateTimeType.now());
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
                    .withAdditionalHeader(HEADER_REQUEST_ID, queryId.toString())
                    .execute();

            if (response.getEntry().size() > 1) {
              logger.error("Found {} (more than one) Patient with identifier {}", response.getEntry().size(), Helper.sanitizeString(poi.getIdentifier()));
            } else if (response.getEntry().size() == 0) {
              logger.error("Did not find any Patient with identifier {}", Helper.sanitizeString(poi.getIdentifier()));
              return null;
            }

            if (response.getEntry().size() > 0) {
              Patient patient = (Patient) response.getEntryFirstRep().getResource();
              tenantService.saveDataTraces(queryId, patient.getIdPart(), List.of(patient));
              patient.getMeta().addExtension(Constants.ReceivedDateExtensionUrl, DateTimeType.now());
              patientMap.put(poi.getIdentifier(), patient);
              poi.setId(patient.getIdElement().getIdPart());
              return patient;
            }
          }
        } catch (ReportFailureException e) {
          throw e;
        } catch (Exception e) {
          logger.error("Unable to retrieve patient with identifier " + Helper.sanitizeString(poi.toString()), e);
        } finally {
          semaphore.release();
          int completed = progress.incrementAndGet();
          double percent = (completed * 100.0) / patientsOfInterest.size();
          logger.info("Progress ({}%) for Patient Resource {} is {} of {}", String.format("%.1f", percent), context.getMasterIdentifierValue(), completed, patientsOfInterest.size());
        }
        return null;
      }).collect(Collectors.toList())).get();
    } catch (ExecutionException e) {
      if (e.getCause() instanceof ReportFailureException) {
        throw (ReportFailureException) e.getCause();
      }
    } catch (Exception e) {
      logger.error("Error retrieving Patient resources: {}", e.getMessage(), e);
      return;
    }

    progress.set(0);

    try {
      var contextMapCopy = MDC.getCopyOfContextMap();
      // loop through the patient ids to retrieve the patientData using each patient.
      List<Patient> patients = new ArrayList<>(patientMap.values());

      patientDataFork.submit(() -> patients.parallelStream().map(patient -> {
        semaphore.acquireUninterruptibly();

        try {
          MDC.setContextMap(contextMapCopy);
          logger.debug(String.format("Beginning to load data for patient with logical ID %s", patient.getIdElement().getIdPart()));
          PatientData patientData = this.loadInitialPatientData(criteria, context, patient);
          this.storePatientData(criteria, context, patient.getIdElement().getIdPart(), patientData.getBundle());
        } catch (ReportFailureException e) {
          throw e;
        } catch (Exception ex) {
          logger.error("Error loading patient data for patient {}: {}", patient.getId(), ex.getMessage(), ex);
          return null;
        } finally {
          semaphore.release();
          int completed = progress.incrementAndGet();
          double percent = (completed * 100.0) / patients.size();
          logger.info("Progress ({}%) for Initial Patient Data {} is {} of {}", String.format("%.1f", percent), context.getMasterIdentifierValue(), completed, patients.size());
        }

        return patient.getIdElement().getIdPart();
      }).collect(Collectors.toList())).get();
    } catch (ExecutionException e) {
      if (e.getCause() instanceof ReportFailureException) {
        throw (ReportFailureException) e.getCause();
      }
    } catch (Exception e) {
      logger.error("Error scooping data for patients {}", e.getMessage(), e);
    }
  }

  public void loadSupplementalPatientData(ReportCriteria criteria, ReportContext context, List<PatientOfInterestModel> patientsOfInterest) {
    ForkJoinPool patientDataFork = ForkJoinPool.commonPool();
    AtomicInteger progress = new AtomicInteger(0);
    Semaphore semaphore = getSemaphore();

    try {
      var contextMapCopy = MDC.getCopyOfContextMap();
      patientDataFork.submit(() -> patientsOfInterest.parallelStream().map(poi -> {
        semaphore.acquireUninterruptibly();

        try {
          MDC.setContextMap(contextMapCopy);
          logger.debug(String.format("Continuing to load data for patient with logical ID %s", poi.getId()));
          Bundle patientBundle = com.lantanagroup.link.db.model.PatientData.asBundle(this.tenantService.findPatientData(context.getMasterIdentifierValue(), poi.getId()));
          PatientData patientData = this.loadSupplementalPatientData(criteria, context, poi.getId(), patientBundle);
          this.storePatientData(criteria, context, poi.getId(), patientData.getBundle());
        } catch (ReportFailureException e) {
          throw e;
        } catch (Exception ex) {
          logger.error("Error loading patient data for patient {}: {}", poi.getId(), ex.getMessage(), ex);
          return null;
        } finally {
          semaphore.release();
          int completed = progress.incrementAndGet();
          double percent = (completed * 100.0) / patientsOfInterest.size();
          logger.info("Progress ({}%) for Supplemental Patient Data {} is {} of {}", String.format("%.1f", percent), context.getMasterIdentifierValue(), completed, patientsOfInterest.size());
        }

        return poi.getId();
      }).collect(Collectors.toList())).get();
    } catch (ExecutionException e) {
      if (e.getCause() instanceof ReportFailureException) {
        throw (ReportFailureException) e.getCause();
      }
    } catch (Exception e) {
      logger.error("Error scooping data for patients {}", e.getMessage(), e);
    }
  }

  private void storePatientData(ReportCriteria criteria, ReportContext context, String patientId, Bundle patientBundle) {
    try {
      eventService.triggerDataEvent(this.tenantService, EventTypes.AfterPatientDataQuery, patientBundle, criteria, context, null);

      this.applyConceptMaps.execute(this.tenantService, patientBundle);

      eventService.triggerDataEvent(this.tenantService, EventTypes.AfterApplyConceptMaps, patientBundle, criteria, context, null);

      List<com.lantanagroup.link.db.model.PatientData> dbPatientData = patientBundle.getEntry().stream().map(entry -> {
        com.lantanagroup.link.db.model.PatientData dbpd = new com.lantanagroup.link.db.model.PatientData();
        dbpd.setDataTraceId(DataTrace.getId(entry.getResource()));
        dbpd.setPatientId(patientId);
        dbpd.setResourceType(entry.getResource().getResourceType().toString());
        dbpd.setResourceId(entry.getResource().getIdElement().getIdPart());
        dbpd.setResource(entry.getResource());
        return dbpd;
      }).collect(Collectors.toList());

      eventService.triggerDataEvent(this.tenantService, EventTypes.BeforePatientDataStore, patientBundle, criteria, context, null);

      if (this.shouldPersist) {
        logger.info("Storing patient data for patient {}", patientId);

        // store data
        //noinspection unused
        try (Stopwatch stopwatch = this.stopwatchManager.start(Constants.TASK_STORE_PATIENT_DATA, Constants.CATEGORY_QUERY)) {
          this.tenantService.savePatientData(context.getMasterIdentifierValue(), dbPatientData);
        }
      } else {
        logger.info("Not storing patient data bundle");
      }

      eventService.triggerDataEvent(this.tenantService, EventTypes.AfterPatientDataStore, patientBundle, criteria, context, null);
    } catch (Exception ex) {
      logger.error("Error storing patient data", ex);
    }
  }
}
