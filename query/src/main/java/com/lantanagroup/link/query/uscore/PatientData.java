package com.lantanagroup.link.query.uscore;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.*;
import com.lantanagroup.link.config.query.USCoreConfig;
import com.lantanagroup.link.config.query.USCoreOtherResourceTypeConfig;
import com.lantanagroup.link.config.query.USCoreQueryParametersResourceConfig;
import com.lantanagroup.link.config.query.USCoreQueryParametersResourceParameterConfig;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class PatientData {
  private static final Logger logger = LoggerFactory.getLogger(PatientData.class);

  private final ReportCriteria criteria;
  private final ReportContext context;
  private final Patient patient;
  private final String patientId;
  private final IGenericClient fhirQueryServer;
  private final USCoreConfig usCoreConfig;
  // private final QueryConfig queryConfig;
  private List<String> resourceTypes;
  private Bundle bundle = new Bundle();
  private List<String> encounterReferences = new ArrayList<>();
  private EventService eventService;
  private HashMap<String, Resource> otherResources;
  private StopwatchManager stopwatchManager;

  public PatientData(StopwatchManager stopwatchManager, HashMap<String, Resource> otherResources, EventService eventService, IGenericClient fhirQueryServer, ReportCriteria criteria, ReportContext context, Patient patient, USCoreConfig usCoreConfig, List<String> resourceTypes) {
    this.stopwatchManager = stopwatchManager;
    this.otherResources = otherResources;
    this.eventService = eventService;
    this.fhirQueryServer = fhirQueryServer;
    this.criteria = criteria;
    this.context = context;
    this.patient = patient;
    this.patientId = patient.getIdElement().getIdPart();
    this.usCoreConfig = usCoreConfig;
    this.resourceTypes = resourceTypes;

    this.bundle.setType(BundleType.TRANSACTION);
    this.bundle.setIdentifier(new Identifier().setValue(this.patientId));
    this.bundle.addEntry()
            .setResource(this.patient)
            .getRequest()
            .setMethod(Bundle.HTTPVerb.PUT)
            .setUrl("Patient/" + patient.getIdElement().getIdPart());
  }

  public Bundle getBundle() {
    return this.bundle;
  }

  /**
   * Only return the ISO time precise to the day
   *
   * @param value
   * @return
   */
  public static String getDateTimeString(String value) {
    if (value != null && value.length() > 10) {
      return value.substring(0, 10);
    }
    return value;
  }

  public static String getQueryParamValue(String value, ReportCriteria criteria, java.time.Period lookBackPeriod) {
    String lookBackStart = criteria.getPeriodStart();

    if (lookBackPeriod != null) {
      Instant lookBackStartInstant = new DateTimeType(criteria.getPeriodStart()).getValue().toInstant().minus(lookBackPeriod);
      lookBackStart = DateTimeFormatter.ISO_INSTANT.format(lookBackStartInstant);
    }

    String ret = value
            .replace("${lookBackStart}", getDateTimeString(lookBackStart))
            .replace("${periodStart}", getDateTimeString(criteria.getPeriodStart()))
            .replace("${periodEnd}", getDateTimeString(criteria.getPeriodEnd()));
    return URLEncoder.encode(ret, StandardCharsets.UTF_8);
  }

  public List<String> getQuery(List<String> measureIds, String resourceType, String patientId) {
    String finalResourceType = resourceType;
    ArrayList<String> params = new ArrayList<>(List.of("patient=Patient/" + URLEncoder.encode(patientId, StandardCharsets.UTF_8)));
    HashMap<String, List<USCoreQueryParametersResourceConfig>> queryParameters = this.usCoreConfig.getQueryParameters();

    //check if queryParameters exist in config, if not just load patient without observations
    for (String measureId : measureIds) {
      if (queryParameters != null && !queryParameters.isEmpty()) {
        if (this.usCoreConfig.getQueryParameters() != null && this.usCoreConfig.getQueryParameters().containsKey(measureId)) {

          List<USCoreQueryParametersResourceConfig> resourceQueryParams =
                  this.usCoreConfig.getQueryParameters()
                          .get(measureId)
                          .stream()
                          .filter(queryParams -> queryParams.getResourceType().equals(finalResourceType))
                          .collect(Collectors.toList());

          for (USCoreQueryParametersResourceConfig resourceQueryParam : resourceQueryParams) {
            if(resourceQueryParam.getParameters() != null){
              for (USCoreQueryParametersResourceParameterConfig param : resourceQueryParam.getParameters()) {
                if (param.getSingleParam() != null && param.getSingleParam()) {
                  List<String> values = param.getValues().stream().map(v -> getQueryParamValue(v, this.criteria, this.usCoreConfig.getLookbackPeriod())).collect(Collectors.toList());
                  String paramValue = String.join(",", values);
                  params.add(param.getName() + "=" + paramValue);
                } else {
                  for (String paramValue : param.getValues()) {
                    params.add(param.getName() + "=" + getQueryParamValue(paramValue, criteria, this.usCoreConfig.getLookbackPeriod()));
                  }
                }
              }
            }
          }
        }
      }
    }

    String query = resourceType + "?" + String.join("&", params);

    // %24%7Bencounter%7D is encoded for "${encounter}" by getQueryParamValue()
    if (query.indexOf("%24%7Bencounter%7D") >= 0) {
      if (resourceType.equals("Encounter")) {
        throw new IllegalArgumentException("Cannot create a query for Encounter that filters by encounter");
      }

      // Create a separate query for each encounter
      List<String> encQueries = this.encounterReferences.stream().map(encRef -> {
        return query.replace("%24%7Bencounter%7D", URLEncoder.encode(encRef, StandardCharsets.UTF_8));
      }).collect(Collectors.toList());
      return encQueries;
    }

    return List.of(query);
  }

  private Bundle rawSearch(String query) {
    int interceptors = 0;

    if (this.fhirQueryServer.getInterceptorService() != null) {
      interceptors = this.fhirQueryServer.getInterceptorService().getAllRegisteredInterceptors().size();
    }

    try {
      logger.info("Executing query: " + query);

      if (this.fhirQueryServer == null) {
        logger.error("Client is null");
      }

      Bundle retBundle = new Bundle();
      Bundle nextBundle = this.fhirQueryServer.search()
              .byUrl(query)
              .returnBundle(Bundle.class)
              .execute();
      addSearchResults(nextBundle, retBundle);

      while (nextBundle.getLink(IBaseBundle.LINK_NEXT) != null) {
        nextBundle = this.fhirQueryServer.loadPage()
                .next(nextBundle)
                .execute();
        addSearchResults(nextBundle, retBundle);
      }

      retBundle.setTotal(retBundle.getEntry().size());
      logger.info(query + " Found " + retBundle.getTotal() + " resources for query " + query);

      if (this.eventService != null) {
        this.eventService.triggerDataEvent(EventTypes.AfterPatientResourceQuery, retBundle, this.criteria, this.context, null);
      }

      return retBundle;
    } catch (Exception ex) {
      logger.error("Could not retrieve \"" + query + "\" from FHIR server " + this.fhirQueryServer.getServerBase() + " with " + interceptors + ": " + ex.getMessage(), ex);

      // TODO - revisit
      // ALM 10May2023 - if we get a socket timeout just return an empty Bundle, Seeing some Epic calls take > 2 minutes
      if (SocketTimeoutException.class.isInstance(ex.getCause())) {
        Bundle retBundle = new Bundle();
        retBundle.setTotal(0);
        return retBundle;
      }
    }

    return null;
  }

  private void addSearchResults(Bundle searchset, Bundle patientData) {
    IParser parser = FhirContextProvider.getFhirContext().newXmlParser();
    for (Bundle.BundleEntryComponent entry : searchset.getEntry()) {
      Resource resource = entry.getResource();
      if (resource instanceof OperationOutcome) {
        logger.warn(parser.encodeResourceToString(resource));
        continue;
      }
      patientData.addEntry().setResource(resource);
    }
  }

  /**
   * Loads encounters for the patient first and populates encounterReferences
   * So that encounterReferences may be used to filter queries using ${encounter}
   *
   * @param measureIds
   */
  private void loadEncounters(List<String> measureIds) {
    if (this.resourceTypes.indexOf("Encounter") < 0) {
      return;
    }

    List<String> encounterQuery = this.getQuery(measureIds, "Encounter", this.patientId);
    Bundle encounterBundle = this.rawSearch(encounterQuery.get(0));
    this.bundle.getEntry().addAll(encounterBundle.getEntry());

    // Create references to all the encounters found
    if (encounterBundle != null) {
      encounterBundle.getEntry().forEach(encEntry -> {
        if (encEntry.getResource() == null || encEntry.getResource().getResourceType() != ResourceType.Encounter) {
          return;
        }

        this.encounterReferences.add("Encounter/" + encEntry.getResource().getIdElement().getIdPart());
      });
    }
  }

  public void loadData(List<String> measureIds) {
    if (resourceTypes.size() == 0) {
      logger.error("Not querying for any patient data.");
      return;
    }

    // Load all encounters first to populate this.encounterReferences
    // so that the encounter ids may be used by getQuery()
    this.loadEncounters(measureIds);
    if (this.encounterReferences.isEmpty() && usCoreConfig.isEncounterBased()) {
      logger.info("No encounters found; exiting query phase");
      return;
    }

    //Loop through resource types specified. If observation, use config to add individual category queries
    Set<String> queryString = new HashSet<>();
    for (String resource : this.resourceTypes) {
      if (resource.equals("Encounter")) continue;   // Skip encounters because they were loaded earlier
      queryString.addAll(this.getQuery(measureIds, resource, this.patientId));
    }

    if (!queryString.isEmpty()) {
      try {
        queryString.forEach(query -> {
          Bundle bundle = this.rawSearch(query);
          if (bundle != null) {
            this.bundle.getEntry().addAll(bundle.getEntry());
          }
        });
      }
      catch(Exception ex) {
        logger.error("Error while parallel processing patient data queries: {}", Helper.encodeLogging(ex.getMessage()));
      }
    } else {
      logger.warn("No queries generated based on resource types and configuration");
    }

    Stopwatch stopwatch = this.stopwatchManager.start("query-resources-other");
    this.getOtherResources();
    stopwatch.stop();
  }

  private List<List<String>> separateByCount(List<String> resourceIds, Integer max) {
    List<String> queue = List.copyOf(resourceIds); // So we don't modify the original List by ref
    List<List<String>> ret = new Stack<>();
    List<String> next = new Stack<>();

    for (int i = 0; i < queue.size(); i++) {
      next.add(queue.get(i));

      if (next.size() >= max || i == queue.size() - 1) {
        ret.add(next);
        next = new Stack<>();
      }
    }

    return ret;
  }

  private void getOtherResources() {
    List<Reference> references = ResourceIdChanger.findReferences(this.bundle);

    if (this.usCoreConfig.getOtherResourceTypes() != null) {
      HashMap<String, List<String>> resourcesToGet = new HashMap<>();

      for (Reference reference : references) {
        if (!reference.hasReference()) {
          continue;
        }

        String[] refParts = reference.getReference().split("/");
        List<String> otherResourceTypes = this.usCoreConfig.getOtherResourceTypes().stream()
                .map(ort -> ort.getResourceType())
                .collect(Collectors.toList());

        if (otherResourceTypes.contains(refParts[0])) {
          if (!resourcesToGet.containsKey(refParts[0])) {
            resourcesToGet.put(refParts[0], new ArrayList<>());
          }

          List<String> resourceIds = resourcesToGet.get(refParts[0]);

          if (!resourceIds.contains(refParts[1])) {
            resourceIds.add(refParts[1]);
          }
        }
      }

      resourcesToGet.keySet().stream().forEach(resourceType -> {
        Stopwatch stopwatch = this.stopwatchManager.start(String.format("query-resources-other-%s", resourceType));
        List<String> allResourceIds = resourcesToGet.get(resourceType);

        // Determine if other resource was already retrieved by as part of another patient query
        for (int i = allResourceIds.size() - 1; i >= 0; i--) {
          String reference = resourceType + "/" + allResourceIds.get(i);
          if (this.otherResources.containsKey(reference)) {
            this.bundle.getEntry().add(new Bundle.BundleEntryComponent().setResource(this.otherResources.get(reference)));
            allResourceIds.remove(i);
          }
        }

        logger.info("Loading {} other {} resources for patient {}", allResourceIds.size(), resourceType, patientId);
        USCoreOtherResourceTypeConfig otherResourceTypeConfig = this.usCoreConfig.getOtherResourceTypes().stream()
                .filter(ort -> ort.getResourceType().equals(resourceType))    // If we got to this point, we know a resourceType exists in the config that matches
                .findFirst().get();

        if (otherResourceTypeConfig.getSupportsSearch()) {
          Integer countPerSearch = otherResourceTypeConfig.getCountPerSearch() != null ? otherResourceTypeConfig.getCountPerSearch() : 100;
          List<List<String>> dividedQueries = this.separateByCount(allResourceIds, countPerSearch);

          for (List<String> resourceIds : dividedQueries) {
            Bundle otherResources = this.fhirQueryServer.search()
                    .forResource(resourceType)
                    .where(Resource.RES_ID.exactly().codes(resourceIds))
                    .returnBundle(Bundle.class)
                    .execute();

            otherResources.getEntry().forEach(e -> {
              this.otherResources.put(e.getResource().getResourceType().toString() + "/" + e.getResource().getIdElement().getIdPart(), e.getResource());
              this.bundle.addEntry().setResource(e.getResource());
            });
          }
        } else {
          resourcesToGet.get(resourceType).parallelStream().forEach(resourceId -> {
            try {
              Resource resource = (Resource) this.fhirQueryServer.read()
                      .resource(resourceType)
                      .withId(resourceId)
                      .execute();

              this.otherResources.put(resource.getResourceType().toString() + "/" + resource.getIdElement().getIdPart(), resource);
              this.bundle.addEntry().setResource(resource);
            } catch (Exception e) {
              logger.debug("Can't find resource of type: " + resourceType + " and id: " + resourceId);
            }
          });
        }

        stopwatch.stop();
      });
    }
  }
}
