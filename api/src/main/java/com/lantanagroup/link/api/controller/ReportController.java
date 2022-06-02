package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.*;
import com.lantanagroup.link.api.ReportGenerator;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.auth.OAuth2Helper;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.config.api.ApiConfigEvents;
import com.lantanagroup.link.config.api.ApiQueryConfigModes;
import com.lantanagroup.link.config.auth.LinkOAuthConfig;
import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.model.*;
import com.lantanagroup.link.nhsn.FHIRReceiver;
import com.lantanagroup.link.query.IQuery;
import com.lantanagroup.link.query.QueryFactory;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.util.Strings;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/report")
public class ReportController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(ReportController.class);
  private static final String PeriodStartParamName = "periodStart";
  private static final String PeriodEndParamName = "periodEnd";
  private static boolean reportSent = false;

  @Autowired
  @Setter
  private ApiConfig config;

  @Autowired
  @Setter
  private ApiConfigEvents apiConfigEvents;

  @Autowired
  private ApplicationContext context;

  @Autowired
  private QueryConfig queryConfig;


  private void storeReportBundleResources(Bundle bundle, ReportContext context) {
    Optional<Bundle.BundleEntryComponent> measureEntry = bundle.getEntry().stream()
            .filter(e -> e.getResource().getResourceType() == ResourceType.Measure)
            .findFirst();

    if (measureEntry.isPresent()) {
      Measure measure = (Measure) measureEntry.get().getResource();
      context.setMeasureId(measure.getIdElement().getIdPart());
      context.setMeasure(measure);
    }

    // Make sure each entry in the bundle has a request
    bundle.getEntry().forEach(entry -> {
      if (entry.getRequest() == null) {
        entry.setRequest(new Bundle.BundleEntryRequestComponent());
      }

      if (entry.getResource() != null && entry.getResource().getIdElement() != null && StringUtils.isNotEmpty(entry.getResource().getIdElement().getIdPart())) {
        if (entry.getRequest().getMethod() == null) {
          entry.getRequest().setMethod(Bundle.HTTPVerb.PUT);
        }

        if (StringUtils.isEmpty(entry.getRequest().getUrl())) {
          entry.getRequest().setUrl(entry.getResource().getResourceType().toString() + "/" + entry.getResource().getIdElement().getIdPart());
        }
      }
    });

    logger.info("Executing the measure definition bundle");

    // Store the resources of the measure on the evaluation service
    bundle.setType(Bundle.BundleType.BATCH);
    bundle = FhirHelper.storeTerminologyAndReturnOther(bundle, this.config);
    FhirDataProvider fhirDataProvider = new FhirDataProvider(this.config.getEvaluationService());
    fhirDataProvider.transaction(bundle);

    logger.info("Done executing the measure definition bundle");
  }

  private void resolveMeasure(ReportCriteria criteria, ReportContext context) throws Exception {
    String reportDefIdentifierSystem = criteria.getReportDefIdentifier() != null && criteria.getReportDefIdentifier().indexOf("|") >= 0 ?
            criteria.getReportDefIdentifier().substring(0, criteria.getReportDefIdentifier().indexOf("|")) : "";
    String reportDefIdentifierValue = criteria.getReportDefIdentifier() != null && criteria.getReportDefIdentifier().indexOf("|") >= 0 ?
            criteria.getReportDefIdentifier().substring(criteria.getReportDefIdentifier().indexOf("|") + 1) :
            criteria.getReportDefIdentifier();

    // Find the report definition bundle for the given ID
    Bundle reportDefBundle = this.getFhirDataProvider().findBundleByIdentifier(reportDefIdentifierSystem, reportDefIdentifierValue);

    if (reportDefBundle == null) {
      throw new Exception("Did not find report definition with ID " + criteria.getReportDefId());
    }

    // check if the remote report definition build is newer
    SimpleDateFormat formatter = new SimpleDateFormat(Helper.RFC_1123_DATE_TIME_FORMAT);
    String lastUpdateDate = formatter.format(reportDefBundle.getMeta().getLastUpdated());

    String url = this.queryConfig.getFhirServerBase() + "/" + reportDefBundle.getResourceType() + "/" + reportDefBundle.getEntryFirstRep().getResource().getIdElement().getIdPart();
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .setHeader("if-modified-since", lastUpdateDate);

    //check if report-defs config has auth properties, if so generate token and add to request
    LinkOAuthConfig authConfig = config.getReportDefs().getAuth();
    if (authConfig != null) {
      try {
        String token = OAuth2Helper.getToken(authConfig);
        requestBuilder.setHeader("Authorization", "Bearer " + token);
      } catch (Exception ex) {
        logger.error(String.format("Error generating authorization token: %s", ex.getMessage()));
        return;
      }
    }

    HttpRequest request = requestBuilder.build();

    HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

    logger.info(String.format("Checked the latest version of the Measure bundle: %s", reportDefBundle.getResourceType() + "/" + reportDefBundle.getEntryFirstRep().getResource().getIdElement().getIdPart()));

    if (response.statusCode() == 200) {
      Bundle reportRemoteReportDefBundle = FhirHelper.getBundle(response.body());
      String missingResourceTypes = "";
      if (reportRemoteReportDefBundle == null) {
        logger.error(String.format("Error parsing report def bundle from %s", url));
      } else {
        missingResourceTypes = FhirHelper.getQueryConfigurationDataReqMissingResourceTypes(FhirHelper.getQueryConfigurationResourceTypes(queryConfig), reportRemoteReportDefBundle);
        if (!missingResourceTypes.equals("")) {
          logger.error(String.format("These resource types %s are in data requirements but missing from the configuration.", missingResourceTypes));
        }
      }
      if (reportRemoteReportDefBundle != null && !missingResourceTypes.equals("")) {
        String latestDate = formatter.format(reportRemoteReportDefBundle.getMeta().getLastUpdated());
        logger.info(String.format("Acquired the latest Measure bundle %s with the date of: %s", reportDefBundle.getResourceType() + "/" + reportDefBundle.getEntryFirstRep().getResource().getIdElement().getIdPart(), latestDate));
        reportRemoteReportDefBundle.setId(reportDefBundle.getIdElement().getIdPart());
        reportRemoteReportDefBundle.setMeta(reportDefBundle.getMeta());
        this.getFhirDataProvider().updateResource(reportRemoteReportDefBundle);
        reportDefBundle = reportRemoteReportDefBundle;
        logger.info(String.format("Stored the latest Measure bundle %s with the date of: %s", reportDefBundle.getResourceType() + "/" + reportDefBundle.getEntryFirstRep().getResource().getIdElement().getIdPart(), latestDate));
      }
    } else {
      logger.info("The latest version of the Measure bundle is already stored. There is no need to re-acquire it.");
    }

    try {
      // Store the resources in the report definition bundle on the internal FHIR server
      logger.info("Storing the resources for the report definition " + criteria.getReportDefId());
      this.storeReportBundleResources(reportDefBundle, context);
      context.setReportDefBundle(reportDefBundle);
    } catch (Exception ex) {
      logger.error("Error storing resources for the report definition " + criteria.getReportDefId() + ": " + ex.getMessage());
      throw new Exception("Error storing resources for the report definition: " + ex.getMessage(), ex);
    }
  }

  private List<PatientOfInterestModel> getPatientIdentifiers(ReportCriteria criteria, ReportContext context) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    if (context.getPatientCensusLists() != null && context.getPatientCensusLists().size() > 0) {
      List<PatientOfInterestModel> patientOfInterestModelList = new ArrayList<>();
      for(ListResource censusList : context.getPatientCensusLists()) {
        for(ListResource.ListEntryComponent censusPatient : censusList.getEntry()) {
          PatientOfInterestModel patient = new PatientOfInterestModel(censusPatient.getItem().getReference(),
                  censusPatient.getItem().getIdentifier().getSystem() + "|" + censusPatient.getItem().getIdentifier().getValue());
          patientOfInterestModelList.add(patient);
        }
      }
      context.setPatientsOfInterest(patientOfInterestModelList);
      return patientOfInterestModelList;
    }
    else {
      IPatientIdProvider provider;
      Class<?> senderClass = Class.forName(this.config.getPatientIdResolver());
      Constructor<?> patientIdentifierConstructor = senderClass.getConstructor();
      provider = (IPatientIdProvider) patientIdentifierConstructor.newInstance();
      return provider.getPatientsOfInterest(criteria, context, this.config);
    }
  }

  private List<QueryResponse> getRemotePatientData(List<PatientOfInterestModel> patientsOfInterest) {
    try {
      URL url = new URL(new URL(this.config.getQuery().getUrl()), "/api/data");
      URIBuilder uriBuilder = new URIBuilder(url.toString());

      patientsOfInterest.forEach(poi -> {
        if (poi.getReference() != null) {
          uriBuilder.addParameter("patientRef", poi.getReference());
        } else if (poi.getIdentifier() != null) {
          uriBuilder.addParameter("patientIdentifier", poi.getIdentifier());
        }
      });

      logger.info("Scooping data remotely for the patients: " + StringUtils.join(patientsOfInterest, ", ") + " from: " + uriBuilder);

      HttpRequest request = HttpRequest.newBuilder()
              .uri(uriBuilder.build().toURL().toURI())
              .header("Authorization", "Key " + this.config.getQuery().getApiKey())
              .build();
      HttpClient client = HttpClient.newHttpClient();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        throw new Exception(String.format("Response from remote query was %s: %s", response.statusCode(), response.body()));
      }

      String responseBody = response.body();
      Bundle bundle = (Bundle) this.ctx.newJsonParser().parseResource(responseBody);

      List<QueryResponse> queryResponses = new ArrayList<>();
      QueryResponse queryResponse = new QueryResponse();
      for (Bundle.BundleEntryComponent e : bundle.getEntry()) {
        if (e.getResource().getResourceType() == ResourceType.Patient) {
          queryResponse = new QueryResponse(e.getResource().getIdElement().getIdPart(), new Bundle());
          queryResponses.add(queryResponse);
        }

        if (queryResponse.getBundle() != null) {
          queryResponse.getBundle().addEntry(e);
        }
      }

      return queryResponses;
    } catch (Exception ex) {
      logger.error("Error retrieving remote patient data: " + ex.getMessage());
    }

    return null;
  }


  private List<ConceptMap> getConceptMaps() {
    List<ConceptMap> conceptMapsList = new ArrayList();
    if (this.config.getConceptMaps() != null) {
      // get it from fhirserver
      this.config.getConceptMaps().stream().forEach(concepMapId -> {
        IBaseResource conceptMap = getFhirDataProvider().getResourceByTypeAndId("ConceptMap", concepMapId);
        conceptMapsList.add((ConceptMap) conceptMap);
      });
    }
    return conceptMapsList;
  }

  /**
   * Executes the configured query implementation against a list of POIs. The POI at the start of this
   * may be either identifier (such as MRN) or logical id for the FHIR Patient resource.
   *
   * @param patientsOfInterest
   * @return Returns a list of the logical ids for the Patient resources stored on the internal fhir server
   * @throws Exception
   */

  private List<QueryResponse> queryAndStorePatientData(List<PatientOfInterestModel> patientsOfInterest, List<String> resourceTypes, ReportCriteria criteria, ReportContext context, String reportId) throws Exception {
    try {
      List<QueryResponse> patientQueryResponses = null;

      // Get the data
      if (this.config.getQuery().getMode() == ApiQueryConfigModes.Local) {
        logger.info("Querying/scooping data for the patients: " + StringUtils.join(patientsOfInterest, ", "));
        QueryConfig queryConfig = this.context.getBean(QueryConfig.class);
        IQuery query = QueryFactory.getQueryInstance(this.context, queryConfig.getQueryClass());
        patientQueryResponses = query.execute(patientsOfInterest, resourceTypes);
      } else if (this.config.getQuery().getMode() == ApiQueryConfigModes.Remote) {
        patientQueryResponses = this.getRemotePatientData(patientsOfInterest);
      }

      triggerEvent(EventTypes.AfterPatientDataQuery, criteria, context);

      if (patientQueryResponses == null) {
        throw new Exception("patientDataBundle is null");
      }

      context.setConceptMaps(getConceptMaps());

      triggerEvent(EventTypes.BeforePatientDataStore, criteria, context);

      // store patient data
      for (QueryResponse patientQueryResponse : patientQueryResponses) {
        // Make sure the bundle is a batch - it will load as much as it can
        patientQueryResponse.getBundle().setType(Bundle.BundleType.BATCH);

        patientQueryResponse.getBundle().getEntry().forEach(entry ->
                entry.getRequest()
                        .setMethod(Bundle.HTTPVerb.PUT)
                        .setUrl(entry.getResource().getResourceType().toString() + "/" + entry.getResource().getIdElement().getIdPart())
        );

        patientQueryResponse.getBundle().setId(reportId + "-" + patientQueryResponse.getPatientId().hashCode());

        // Store the data
        logger.info("Storing patient data bundle Bundle/" + patientQueryResponse.getBundle().getId());
        this.getFhirDataProvider().updateResource(patientQueryResponse.getBundle());
      }

      triggerEvent(EventTypes.AfterPatientDataStore, criteria, context);
      return patientQueryResponses;
    } catch (Exception ex) {
      String msg = String.format("Error scooping/storing data for the patients (%s): %s", StringUtils.join(patientsOfInterest, ", "), ex.getMessage());
      logger.error(msg);
      throw new Exception(msg, ex);
    }
  }

  private DocumentReference getDocumentReferenceByMeasureAndPeriod(Identifier measureIdentifier, String startDate, String endDate, boolean regenerate) throws Exception {
    return this.getFhirDataProvider().findDocRefByMeasureAndPeriod(measureIdentifier, startDate, endDate);
  }

  @PostMapping("/$generate")
  public GenerateResponse generateReport(
          @AuthenticationPrincipal LinkCredentials user,
          HttpServletRequest request,
          @RequestParam("reportDefIdentifier") String reportDefIdentifier,
          @RequestParam("periodStart") String periodStart,
          @RequestParam("periodEnd") String periodEnd,
          boolean regenerate) {

    GenerateResponse response = new GenerateResponse();
    ReportCriteria criteria = new ReportCriteria(reportDefIdentifier, periodStart, periodEnd);
    ReportContext context = new ReportContext(this.getFhirDataProvider());

    try {

      triggerEvent(EventTypes.BeforeMeasureResolution, criteria, context);

      // Get the latest measure def and update it on the FHIR storage server
      this.resolveMeasure(criteria, context);

      triggerEvent(EventTypes.AfterMeasureResolution, criteria, context);

      // Search the reference document by measure criteria nd reporting period
      DocumentReference existingDocumentReference = this.getDocumentReferenceByMeasureAndPeriod(
              context.getReportDefBundle().getIdentifier(),
              periodStart,
              periodEnd,
              regenerate);
      if (existingDocumentReference != null && !regenerate) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "A report has already been generated for the specified measure and reporting period. Are you sure you want to re-generate the report (re-query the data from the EHR and re-evaluate the measure based on updated data)?");
      }

      if (existingDocumentReference != null) {
        existingDocumentReference = FhirHelper.incrementMinorVersion(existingDocumentReference);

        Class<?> senderClazz = Class.forName(this.config.getSender());
        IReportSender sender = (IReportSender) this.context.getBean(senderClazz);

        Bundle submitted = sender.retrieve(config, this.ctx, existingDocumentReference);
        if(submitted != null && submitted.getEntry().size() > 0) {
          List<ListResource> censusList = new ArrayList<>();
          for(Bundle.BundleEntryComponent entry: submitted.getEntry()) {
            if(entry.getResource().getResourceType() == ResourceType.List) {
              censusList.add((ListResource)entry.getResource());
              this.getFhirDataProvider().updateResource(entry.getResource());
            }
          }
          context.setPatientCensusLists(censusList);
        }
      }

      // Generate the master report id
      String id = "";
      if (!regenerate || existingDocumentReference == null) {
        // generate master report id based on the report date range and the measure used in the report generation
        id = String.valueOf((criteria.getReportDefIdentifier() + "-" + criteria.getPeriodStart() + "-" + criteria.getPeriodEnd()).hashCode());
      } else {
        id = existingDocumentReference.getMasterIdentifier().getValue();
        triggerEvent(EventTypes.OnRegeneration, criteria, context);
      }
      triggerEvent(EventTypes.BeforePatientOfInterestLookup, criteria, context);

      // Get the patient identifiers for the given date
      List<PatientOfInterestModel> patientsOfInterest = this.getPatientIdentifiers(criteria, context);


      triggerEvent(EventTypes.AfterPatientOfInterestLookup, criteria, context);

      // Get the resource types to query
      List<String> resourceTypesToQuery = FhirHelper.getQueryConfigurationDataReqCommonResourceTypes(queryConfig.getPatientResourceTypes(), context.getReportDefBundle());

      // Scoop the data for the patients and store it
      context.getPatientData().addAll(this.queryAndStorePatientData(patientsOfInterest, resourceTypesToQuery, criteria, context, id));

      this.getFhirDataProvider().audit(request, user.getJwt(), FhirHelper.AuditEventTypes.InitiateQuery, "Successfully Initiated Query");

      context.setReportId(id);

      response.setReportId(id);

      ReportGenerator generator = new ReportGenerator(context, criteria, config, user);

      triggerEvent(EventTypes.BeforeMeasureEval, criteria, context);

      List<MeasureReport> reports = generator.generate(criteria, context, context.getPatientData());

      triggerEvent(EventTypes.AfterMeasureEval, criteria, context);


      triggerEvent(EventTypes.BeforeReportStore, criteria, context);

      generator.store(reports, criteria, context, existingDocumentReference);

      triggerEvent(EventTypes.AfterReportStore, criteria, context);

      this.getFhirDataProvider().audit(request, user.getJwt(), FhirHelper.AuditEventTypes.Generate, "Successfully Generated Report");
    } catch (ResponseStatusException rse) {
      logger.error(String.format("Error generating report: %s", rse.getMessage()), rse);
      throw rse;
    } catch (Exception ex) {
      logger.error(String.format("Error generating report: %s", ex.getMessage()), ex);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Please contact system administrator regarding this error.");
    }

    return response;
  }


  /**
   * Sends the specified report to the recipients configured in <strong>api.send-urls</strong>
   *
   * @param reportId - this is the report identifier after generate report was clicked
   * @param request
   * @throws Exception Thrown when the configured sender class is not found or fails to initialize or the reportId it not found
   */
  @GetMapping("/{reportId}/$send")
  public void send(
          Authentication authentication,
          @PathVariable String reportId,
          HttpServletRequest request) throws Exception {

    if (StringUtils.isEmpty(this.config.getSender()))
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Not configured for sending");

    DocumentReference documentReference = this.getFhirDataProvider().findDocRefForReport(reportId);

    documentReference.setDocStatus(DocumentReference.ReferredDocumentStatus.FINAL);
    documentReference.setDate(new Date());
    documentReference = FhirHelper.incrementMajorVersion(documentReference);

    MeasureReport report = this.getFhirDataProvider().getMeasureReportById(reportId);
    Class<?> senderClazz = Class.forName(this.config.getSender());
    IReportSender sender = (IReportSender) this.context.getBean(senderClazz);

    // save the DocumentReference before sending report
    this.getFhirDataProvider().updateResource(documentReference);
    sender.send(report, request, authentication, this.getFhirDataProvider(),
            this.config.getSendWholeBundle() != null ? this.config.getSendWholeBundle() : true,
            this.config.isRemoveGeneratedObservations());


    String submitterName = FhirHelper.getName(((LinkCredentials) authentication.getPrincipal()).getPractitioner().getName());

    logger.info("MeasureReport with ID " + reportId + " submitted by " + submitterName + " on " + new Date());

    this.getFhirDataProvider().audit(request, ((LinkCredentials) authentication.getPrincipal()).getJwt(), FhirHelper.AuditEventTypes.Send, "Successfully Sent Report");

    if (this.config.getDeleteAfterSubmission()) {
      deleteSentData(documentReference);
    }
  }

  private void deleteSentData(DocumentReference documentReference) {
    String masterMeasureReportID = documentReference.getMasterIdentifier().getValue();
    if(documentReference.getContext().getRelated().size() > 0) {
      List<ListResource> censusList = FhirHelper.getCensusLists(documentReference, this.getFhirDataProvider());
      for(ListResource census : censusList) {
        String censusID = census.getId().contains("List/") && census.getId().contains("/_history")?
                census.getId().substring("List/".length(), census.getId().indexOf("/_history")):census.getId().contains("List/")?
                census.getId().substring("List/".length()):census.getId();;
        for(ListResource.ListEntryComponent entry: census.getEntry()) {
          if(entry.getItem().getReference() != null) {
            String patientRef = entry.getItem().getReference().contains("Patient/")?
                    entry.getItem().getReference().substring("Patient/".length()):entry.getItem().getReference();
            String patientReportID = String.valueOf(patientRef.hashCode());

            try {
              this.getFhirDataProvider().deleteResource("Bundle", masterMeasureReportID + "-" + patientReportID, true);
            } catch (Exception e) {
              logger.error(e.getMessage());
            }

            try {
              this.getFhirDataProvider().deleteResource("MeasureReport", masterMeasureReportID + "-" + patientReportID, true);
            } catch (Exception e) {
              logger.error(e.getMessage());
            }
          }
        }
        try {
          this.getFhirDataProvider().deleteResource("List", censusID, true);
        } catch (Exception e) {
          logger.error(e.getMessage());
        }
      }
    }
    reportSent = true;
  }

  @GetMapping("/{reportId}/$download")
  public void download(
          @PathVariable String reportId,
          HttpServletResponse response,
          Authentication authentication,
          HttpServletRequest request) throws Exception {

    if (StringUtils.isEmpty(this.config.getDownloader()))
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Not configured for downloading");

    IReportDownloader downloader;
    Class<?> downloaderClass = Class.forName(this.config.getDownloader());
    Constructor<?> downloaderCtor = downloaderClass.getConstructor();
    downloader = (IReportDownloader) downloaderCtor.newInstance();

    downloader.download(reportId, this.getFhirDataProvider(), response, this.ctx, this.config);

    this.getFhirDataProvider().audit(request, ((LinkCredentials) authentication.getPrincipal()).getJwt(), FhirHelper.AuditEventTypes.Export, "Successfully Exported Report for Download");
  }

  @GetMapping(value = "/{reportId}")
  public ReportModel getReport(
          @PathVariable("reportId") String reportId) {

    ReportModel report = new ReportModel();

    DocumentReference documentReference = this.getFhirDataProvider().findDocRefForReport(reportId);
    report.setMeasureReport(this.getFhirDataProvider().getMeasureReportById(documentReference.getMasterIdentifier().getValue()));

    FhirDataProvider evaluationDataProvider = new FhirDataProvider(this.config.getEvaluationService());
    Measure measure = evaluationDataProvider.findMeasureByIdentifier(documentReference.getIdentifier().get(0));

    report.setMeasure(measure);

    report.setIdentifier(reportId);
    report.setVersion(documentReference
            .getExtensionByUrl(Constants.DocumentReferenceVersionUrl) != null ?
            documentReference.getExtensionByUrl(Constants.DocumentReferenceVersionUrl).getValue().toString() : null);
    report.setStatus(documentReference.getDocStatus().toString());
    report.setDate(documentReference.getDate());

    if(reportSent) {
      reportSent = false;
      try {
        this.getFhirDataProvider().deleteResource("MeasureReport", reportId, true);
      } catch (Exception e) {
        logger.error(e.getMessage());
      }
    }

    return report;
  }

  @GetMapping(value = "/{reportId}/patient")
  public List<PatientReportModel> getReportPatients(
          @PathVariable("reportId") String reportId) throws Exception {

    String bundleLocation = "";
    Bundle patientBundle = null;
    PatientReportModel report = null;

    List<PatientReportModel> reports = new ArrayList();
    DocumentReference documentReference = this.getFhirDataProvider().findDocRefForReport(reportId);
    bundleLocation = FhirHelper.getFirstDocumentReferenceLocation(documentReference);

    // if document submitted and flag is to delete after submission then get the patients from the submitted bundle
    if (!bundleLocation.equals("") && config.getDeleteAfterSubmission()) {
      // get Patients from bundle
      FHIRReceiver receiver = this.context.getBean(FHIRReceiver.class);
      String content = receiver.retrieveContent(bundleLocation);
      patientBundle = this.ctx.newJsonParser().parseResource(Bundle.class, content);
    } else { // otherwise get them as before
      MeasureReport measureReport = this.getFhirDataProvider().getMeasureReportById(documentReference.getMasterIdentifier().getValue());
      Bundle patientRequest = new Bundle();
      patientRequest.setType(Bundle.BundleType.BATCH);
      // Get the references to the individual patient measure reports from the master
      List<String> patientMeasureReportReferences = FhirHelper.getPatientMeasureReportReferences(measureReport);


      // TODO: Refactor this to grab census, get each patient in the census

      // Retrieve the individual patient measure reports from the server
      List<MeasureReport> patientReports = FhirHelper.getPatientReports(patientMeasureReportReferences, this.getFhirDataProvider());
      for (MeasureReport patientMeasureReport : patientReports) {

        patientRequest.addEntry().setRequest(new Bundle.BundleEntryRequestComponent());
        int index = patientRequest.getEntry().size() - 1;
        patientRequest.getEntry().get(index).getRequest().setMethod(Bundle.HTTPVerb.GET);
        patientRequest.getEntry().get(index).getRequest().setUrl(patientMeasureReport.getSubject().getReference());
      }
      if (patientRequest.hasEntry()) {
        patientBundle = this.getFhirDataProvider().transaction(patientRequest);
      }
    }

    // in both cases add the patients info to patientreportmodel to be displayed in UI
    if (patientBundle != null && !patientBundle.getEntry().isEmpty()) {
      for (Bundle.BundleEntryComponent entry : patientBundle.getEntry()) {
        if (entry.getResource() != null && entry.getResource().getResourceType().toString().equals("Patient")) {
          Patient patient = (Patient) entry.getResource();
          report = FhirHelper.setPatientFields(patient, false);
          reports.add(report);
        }
      }
    }
    return reports;
  }


  @PutMapping(value = "/{id}")
  public void saveReport(
          @PathVariable("id") String id,
          Authentication authentication,
          HttpServletRequest request,
          @RequestBody ReportSaveModel data) throws Exception {

    DocumentReference documentReference = this.getFhirDataProvider().findDocRefForReport(id);

    documentReference = FhirHelper.incrementMinorVersion(documentReference);

    try {
      this.getFhirDataProvider().updateResource(documentReference);
      this.getFhirDataProvider().updateResource(data.getMeasureReport());
    } catch (Exception ex) {
      logger.error(String.format("Error saving changes to report: %s", ex.getMessage()));
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error saving changes to report");
    }

    this.getFhirDataProvider().audit(request, ((LinkCredentials) authentication.getPrincipal()).getJwt(),
            FhirHelper.AuditEventTypes.Send, "Successfully updated MeasureReport with id: " +
                    documentReference.getMasterIdentifier().getValue());
  }

  /**
   * Retrieves data (encounters, conditions, etc.) for the specified patient within the specified report.
   *
   * @param reportId       The report id
   * @param patientId      The patient id within the report
   * @param authentication The authenticated user making the request
   * @param request        The HTTP request
   * @return SubjectReportModel
   * @throws Exception
   */
  @GetMapping(value = "/{reportId}/patient/{patientId}")
  public PatientDataModel getPatientData(
          @PathVariable("reportId") String reportId,
          @PathVariable("patientId") String patientId,
          Authentication authentication,
          HttpServletRequest request) throws Exception {

    String bundleLocation = "";
    PatientDataModel data = new PatientDataModel();

    DocumentReference documentReference = this.getFhirDataProvider().findDocRefForReport(reportId);
    bundleLocation = FhirHelper.getFirstDocumentReferenceLocation(documentReference);
    if (!bundleLocation.equals("") && config.getDeleteAfterSubmission()) {
      FHIRReceiver receiver = this.context.getBean(FHIRReceiver.class);
      String content = receiver.retrieveContent(bundleLocation);
      Bundle patientBundle = this.ctx.newJsonParser().parseResource(Bundle.class, content);

      data.setConditions(new ArrayList<>());
      data.setMedicationRequests(new ArrayList<>());
      data.setProcedures(new ArrayList<>());
      data.setObservations(new ArrayList<>());
      data.setEncounters(new ArrayList<>());
      data.setServiceRequests(new ArrayList());
      if (patientBundle == null || patientBundle.getEntry().isEmpty()) {
        return data;
      }
      for (Bundle.BundleEntryComponent entry : patientBundle.getEntry()) {
        if (entry.getResource() != null && entry.getResource().getResourceType().toString().equals("Condition")) {
          Condition condition = (Condition) entry.getResource();
          if (condition.getSubject().getReference().equals("Patient/" + patientId)) {
            data.getConditions().add(condition);
          }
        }
        if (entry.getResource() != null && entry.getResource().getResourceType().toString().equals("MedicationRequest")) {
          MedicationRequest medicationRequest = (MedicationRequest) entry.getResource();
          if (medicationRequest.getSubject().getReference().equals("Patient/" + patientId)) {
            data.getMedicationRequests().add(medicationRequest);
          }
        }
        if (entry.getResource() != null && entry.getResource().getResourceType().toString().equals("Observation")) {
          Observation observation = (Observation) entry.getResource();
          if (observation.getSubject().getReference().equals("Patient/" + patientId)) {
            data.getObservations().add(observation);
          }
        }
        if (entry.getResource() != null && entry.getResource().getResourceType().toString().equals("Procedure")) {
          Procedure procedure = (Procedure) entry.getResource();
          if (procedure.getSubject().getReference().equals("Patient/" + patientId)) {
            data.getProcedures().add(procedure);
          }
        }
        if (entry.getResource() != null && entry.getResource().getResourceType().toString().equals("Encounter")) {
          Encounter encounter = (Encounter) entry.getResource();
          if (encounter.getSubject().getReference().equals("Patient/" + patientId)) {
            data.getEncounters().add(encounter);
          }
        }
        if (entry.getResource() != null && entry.getResource().getResourceType().toString().equals("ServiceRequest")) {
          ServiceRequest serviceRequest = (ServiceRequest) entry.getResource();
          if (serviceRequest.getSubject().getReference().equals("Patient/" + patientId)) {
            data.getServiceRequests().add(serviceRequest);
          }
        }
      }
    } else {
      MeasureReport measureReport = this.getFhirDataProvider().getMeasureReportById(reportId + "-" + patientId.hashCode());
      List<Reference> evaluatedResources = measureReport.getEvaluatedResource();

      // Conditions
      Bundle conditionBundle = this.getFhirDataProvider().getResources(Condition.SUBJECT.hasId(patientId), "Condition");
      if (conditionBundle.hasEntry()) {
        List<Condition> conditionList = conditionBundle.getEntry().stream()
                .filter(e -> e.getResource() != null)
                .map(e -> (Condition) e.getResource())
                .collect(Collectors.toList());

        conditionList = conditionList.stream()
                .filter(c -> evaluatedResources.stream()
                        .anyMatch(e ->
                                e.getReference().equals(FhirHelper.getIdFromVersion(c.getId()))))
                .collect(Collectors.toList());


        data.setConditions(conditionList);
      }

      // Medications Requests
      Bundle medRequestBundle = this.getFhirDataProvider().getResources(MedicationRequest.SUBJECT.hasId(patientId), "MedicationRequest");
      if (medRequestBundle.hasEntry()) {
        List<MedicationRequest> medicationRequestList = medRequestBundle.getEntry().stream()
                .filter(e -> e.getResource() != null)
                .map(e -> (MedicationRequest) e.getResource())
                .collect(Collectors.toList());

        medicationRequestList = medicationRequestList.stream()
                .filter(m -> evaluatedResources.stream()
                        .anyMatch(e ->
                                e.getReference().equals(FhirHelper.getIdFromVersion(m.getId()))))
                .collect(Collectors.toList());

        data.setMedicationRequests(medicationRequestList);
      }

      // Observations
      Bundle observationBundle = this.getFhirDataProvider().getResources(Observation.SUBJECT.hasId(patientId), "Observation");
      if (observationBundle.hasEntry()) {
        List<Observation> observationList = observationBundle.getEntry().stream()
                .filter(e -> e.getResource() != null)
                .map(e -> (Observation) e.getResource())
                .collect(Collectors.toList());

        observationList = observationList.stream()
                .filter(o -> evaluatedResources.stream()
                        .anyMatch(e ->
                                e.getReference().equals(FhirHelper.getIdFromVersion(o.getId()))))
                .collect(Collectors.toList());

        data.setObservations(observationList);
      }

      // Procedures
      Bundle procedureBundle = this.getFhirDataProvider().getResources(Procedure.SUBJECT.hasId(patientId), "Procedure");
      if (procedureBundle.hasEntry()) {
        List<Procedure> procedureList = procedureBundle.getEntry().stream()
                .filter(e -> e.getResource() != null)
                .map(e -> (Procedure) e.getResource())
                .collect(Collectors.toList());

        procedureList = procedureList.stream()
                .filter(p -> evaluatedResources.stream()
                        .anyMatch(e ->
                                e.getReference().equals(FhirHelper.getIdFromVersion(p.getId()))))
                .collect(Collectors.toList());

        data.setProcedures(procedureList);
      }

      // Encounters
      Bundle encounterBundle = this.getFhirDataProvider().getResources(Encounter.SUBJECT.hasId(patientId), "Encounter");
      if (encounterBundle.hasEntry()) {
        List<Encounter> encounterList = encounterBundle.getEntry().stream()
                .filter(e -> e.getResource() != null)
                .map(e -> (Encounter) e.getResource())
                .collect(Collectors.toList());

        encounterList = encounterList.stream()
                .filter(eL -> evaluatedResources.stream()
                        .anyMatch(e ->
                                e.getReference().equals(FhirHelper.getIdFromVersion(eL.getId()))))
                .collect(Collectors.toList());

        data.setEncounters(encounterList);
      }

      Bundle serviceRequestBundle = this.getFhirDataProvider().getResources(ServiceRequest.SUBJECT.hasId(patientId), "ServiceRequest");
      if (serviceRequestBundle.hasEntry()) {
        List<ServiceRequest> serviceRequestList = serviceRequestBundle.getEntry().stream()
                .filter(sr -> sr.getResource() != null)
                .map(sr -> (ServiceRequest) sr.getResource())
                .collect(Collectors.toList());

        serviceRequestList = serviceRequestList.stream()
                .filter(srL -> evaluatedResources.stream()
                        .anyMatch(e ->
                                e.getReference().equals(FhirHelper.getIdFromVersion(srL.getId()))))
                .collect(Collectors.toList());

        data.setServiceRequests(serviceRequestList);
      }
    }

    return data;
  }

  @DeleteMapping(value = "/{id}")
  public void deleteReport(
          @PathVariable("id") String id,
          Authentication authentication,
          HttpServletRequest request) throws Exception {
    Bundle deleteRequest = new Bundle();

    DocumentReference documentReference = this.getFhirDataProvider().findDocRefForReport(id);

    Extension existingVersionExt = documentReference.getExtensionByUrl(Constants.DocumentReferenceVersionUrl);
    Float existingVersion = Float.parseFloat(existingVersionExt.getValue().toString());
    if(existingVersion >= 1.0f) {
      throw new HttpResponseException(400, "Bad Request, report version is greater than or equal to 1.0");
    }

    // Make sure the bundle is a transaction
    deleteRequest.setType(Bundle.BundleType.TRANSACTION);
    deleteRequest.addEntry().setRequest(new Bundle.BundleEntryRequestComponent());
    deleteRequest.addEntry().setRequest(new Bundle.BundleEntryRequestComponent());
    deleteRequest.getEntry().forEach(entry ->
            entry.getRequest().setMethod(Bundle.HTTPVerb.DELETE)
    );
    String documentReferenceId = documentReference.getId();
    documentReferenceId = documentReferenceId.substring(documentReferenceId.indexOf("/DocumentReference/") + "/DocumentReference/".length(),
            documentReferenceId.indexOf("/_history/"));
    deleteRequest.getEntry().get(0).getRequest().setUrl("MeasureReport/" + documentReference.getMasterIdentifier().getValue());
    deleteRequest.getEntry().get(1).getRequest().setUrl("DocumentReference/" + documentReferenceId);
    this.getFhirDataProvider().transaction(deleteRequest);

    this.getFhirDataProvider().audit(request, ((LinkCredentials) authentication.getPrincipal()).getJwt(),
            FhirHelper.AuditEventTypes.Export, "Successfully deleted DocumentReference" +
                    documentReferenceId + " and MeasureReport " + documentReference.getMasterIdentifier().getValue());
  }

  @GetMapping(value = "/searchReports", produces = {MediaType.APPLICATION_JSON_VALUE})
  public ReportBundle searchReports(
          Authentication authentication,
          HttpServletRequest request,
          @RequestParam(required = false, defaultValue = "1") Integer page,
          @RequestParam(required = false) String bundleId,
          @RequestParam(required = false) String author,
          @RequestParam(required = false) String identifier,
          @RequestParam(required = false) String periodStartDate,
          @RequestParam(required = false) String periodEndDate,
          @RequestParam(required = false) String docStatus,
          @RequestParam(required = false) String submittedDate) {

    Bundle bundle;
    boolean andCond = false;
    ReportBundle reportBundle = new ReportBundle();
    try {
      String url = this.config.getFhirServerStore();
      if (bundleId != null) {
        url += "?_getpages=" + bundleId + "&_getpagesoffset=" + (page - 1) * 20 + "&_count=20";
      } else {
        if (!url.endsWith("/")) url += "/";
        url += "DocumentReference?";
        if (author != null) {
          url += "author=" + author;
          andCond = true;
        }
        if (identifier != null) {
          if (andCond) {
            url += "&";
          }
          url += "identifier=" + Helper.URLEncode(identifier);
          andCond = true;
        }
        if (periodStartDate != null) {
          if (andCond) {
            url += "&";
          }
          url += PeriodStartParamName + "=ge" + periodStartDate;
          andCond = true;
        }
        if (periodEndDate != null) {
          if (andCond) {
            url += "&";
          }
          url += PeriodEndParamName + "=le" + periodEndDate;
          andCond = true;
        }
        if (docStatus != null) {
          if (andCond) {
            url += "&";
          }
          url += "docStatus=" + docStatus.toLowerCase();
        }
        if (submittedDate != null) {
          if (andCond) {
            url += "&";
          }
          Date submittedDateAsDate = Helper.parseFhirDate(submittedDate);
          Date theDayAfterSubmittedDateEnd = Helper.addDays(submittedDateAsDate, 1);
          String theDayAfterSubmittedDateEndAsString = Helper.getFhirDate(theDayAfterSubmittedDateEnd);
          url += "date=ge" + submittedDate + "&date=le" + theDayAfterSubmittedDateEndAsString;
        }
      }


      bundle = this.getFhirDataProvider().fetchResourceFromUrl(url);
      List<Report> lst = bundle.getEntry().parallelStream().map(Report::new).collect(Collectors.toList());
      List<String> reportIds = lst.stream().map(report -> report.getId()).collect(Collectors.toList());
      Bundle response = this.getFhirDataProvider().getMeasureReportsByIds(reportIds);


      response.getEntry().parallelStream().forEach(bundleEntry -> {
        if (bundleEntry.getResource().getResourceType().equals(ResourceType.MeasureReport)) {
          MeasureReport measureReport = (MeasureReport) bundleEntry.getResource();
          Extension extension = measureReport.getExtensionByUrl(Constants.NotesUrl);
          Report foundReport = lst.stream().filter(rep -> rep.getId().equals(measureReport.getIdElement().getIdPart())).findAny().orElse(null);
          if (extension != null && foundReport != null) {
            foundReport.setNote(extension.getValue().toString());
          }
        }
      });
      reportBundle.setReportTypeId(bundleId != null ? bundleId : bundle.getId());
      reportBundle.setList(lst);
      reportBundle.setTotalSize(bundle.getTotal());

      this.getFhirDataProvider().audit(request, ((LinkCredentials) authentication.getPrincipal()).getJwt(), FhirHelper.AuditEventTypes.SearchReports, "Successfully Searched Reports");
    } catch (Exception ex) {
      logger.error(String.format("Error searching Reports: %s", ex.getMessage()), ex);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Please contact system administrator regarding this error");
    }

    return reportBundle;
  }

  /**
   * Retrieves the DocumentReference and MeasureReport, ensures that each of the excluded Patients in the request
   * are listed in the MeasureReport.evaluatedResources or as "excluded" extensions on the MeasureReport. Creates
   * the excluded extension on the MR for each patient, DELETE's each patient. Re-evaluates the MeasureReport against
   * the Measure. Increments the minor version number of the report in DocumentReference. Stores updates to the
   * DR and MR back to the FHIR server.
   *
   * @param authentication   Authentication information to create an IGenericClient to the internal FHIR store
   * @param request          The HTTP request to create an IGenericClient to the internal FHIR store
   * @param user             The user making the request, for the audit trail
   * @param reportId         The ID of the report to re-evaluate after DELETE'ing/excluding the patients.
   * @param excludedPatients A list of patients to be excluded from the report, including reasons for their exclusion
   * @return A ReportModel that has been updated to reflect the exclusions
   * @throws HttpResponseException
   */
  @PostMapping("/{reportId}/$exclude")
  public ReportModel excludePatients(
          Authentication authentication,
          HttpServletRequest request,
          @AuthenticationPrincipal LinkCredentials user,
          @PathVariable("reportId") String reportId,
          @RequestBody List<ExcludedPatientModel> excludedPatients) throws HttpResponseException {

    DocumentReference reportDocRef = this.getFhirDataProvider().findDocRefForReport(reportId);

    if (reportDocRef == null) {
      throw new HttpResponseException(404, String.format("Report %s not found", reportId));
    }


    MeasureReport measureReport = this.getFhirDataProvider().getMeasureReportById(reportId);
    if (measureReport == null) {
      throw new HttpResponseException(404, String.format("Report %s does not have a MeasureReport", reportId));
    }

    if (excludedPatients == null || excludedPatients.size() == 0) {
      throw new HttpResponseException(400, "Not patients indicated to be excluded");
    }

    Measure measure = this.getFhirDataProvider().getMeasureForReport(reportDocRef);

    if (measure == null) {
      logger.error(String.format("The measure for report %s no longer exists on the system", reportId));
      throw new HttpResponseException(500, "Internal Server Error");
    }

    Bundle excludeChangesBundle = new Bundle();
    excludeChangesBundle.setType(Bundle.BundleType.TRANSACTION);
    Boolean changedMeasureReport = false;

    for (ExcludedPatientModel excludedPatient : excludedPatients) {
      if (Strings.isEmpty(excludedPatient.getPatientId())) {
        throw new HttpResponseException(400, String.format("Patient ID not provided for all exclusions"));
      }

      if (excludedPatient.getReason() == null || excludedPatient.getReason().isEmpty()) {
        throw new HttpResponseException(400, String.format("Excluded patient ID %s does not specify a reason", excludedPatient.getPatientId()));
      }

      // Find any references to the Patient in the MeasureReport.evaluatedResources
      List<Reference> foundEvaluatedPatient = measureReport.getEvaluatedResource().stream()
              .filter(er -> er.getReference() != null && er.getReference().equals("Patient/" + excludedPatient.getPatientId()))
              .collect(Collectors.toList());
      // Find any extensions that list the Patient has already being excluded
      Boolean foundExcluded = measureReport.getExtension().stream()
              .filter(e -> e.getUrl().equals(Constants.ExcludedPatientExtUrl))
              .anyMatch(e -> e.getExtension().stream()
                      .filter(nextExt -> nextExt.getUrl().equals("patient") && nextExt.getValue() instanceof Reference)
                      .anyMatch(nextExt -> {
                        Reference patientRef = (Reference) nextExt.getValue();
                        return patientRef.getReference().equals("Patient/" + excludedPatient.getPatientId());
                      }));

      // Throw an error if the Patient does not show up in either evaluatedResources or the excluded extensions
      if (foundEvaluatedPatient.size() == 0 && !foundExcluded) {
        throw new HttpResponseException(400, String.format("Patient %s is not included in report %s", excludedPatient.getPatientId(), reportId));
      }

      // Create an extension for the excluded patient on the MeasureReport
      if (!foundExcluded) {
        Extension newExtension = new Extension(Constants.ExcludedPatientExtUrl);
        newExtension.addExtension("patient", new Reference("Patient/" + excludedPatient.getPatientId()));
        newExtension.addExtension("reason", excludedPatient.getReason());
        measureReport.addExtension(newExtension);
        changedMeasureReport = true;

        // Remove the patient from evaluatedResources, or HAPI will throw a referential integrity exception since it's getting (or has been) deleted
        if (foundEvaluatedPatient.size() > 0) {
          foundEvaluatedPatient.forEach(ep -> measureReport.getEvaluatedResource().remove(ep));
        }
      }

      logger.debug(String.format("Checking if patient %s has been deleted already", excludedPatient.getPatientId()));

      try {
        // Try to GET the patient to see if it has already been deleted or not
        this.getFhirDataProvider().getResource("Patient", excludedPatient.getPatientId());
        logger.debug(String.format("Adding patient %s to list of patients to delete", excludedPatient.getPatientId()));

        // Add a "DELETE" request to the bundle, since it hasn't been deleted
        Bundle.BundleEntryRequestComponent deleteRequest = new Bundle.BundleEntryRequestComponent()
                .setUrl("Patient/" + excludedPatient.getPatientId())
                .setMethod(Bundle.HTTPVerb.DELETE);
        excludeChangesBundle.addEntry().setRequest(deleteRequest);
      } catch (Exception ex) {
        // It's been deleted, just log some debugging info
        logger.debug(String.format("During exclusions for report %s, patient %s is already deleted.", reportId, excludedPatient.getPatientId()));
      }
    }

    if (changedMeasureReport) {
      Bundle.BundleEntryRequestComponent updateMeasureReportReq = new Bundle.BundleEntryRequestComponent()
              .setUrl("MeasureReport/" + reportId)
              .setMethod(Bundle.HTTPVerb.PUT);
      excludeChangesBundle.addEntry()
              .setRequest(updateMeasureReportReq)
              .setResource(measureReport);
    }

    if (excludeChangesBundle.getEntry().size() > 0) {
      logger.debug(String.format("Executing transaction update bundle to delete patients and/or update MeasureReport %s", reportId));

      try {
        this.getFhirDataProvider().transaction(excludeChangesBundle);
      } catch (Exception ex) {
        logger.error(String.format("Error updating resources for report %s to exclude %s patient(s)", reportId, excludedPatients.size()), ex);
        throw new HttpResponseException(500, "Internal Server Error");
      }
    }

    // Create ReportCriteria to be used by MeasureEvaluator
    ReportCriteria criteria = new ReportCriteria(
            measure.getIdentifier().get(0).getSystem() + "|" + measure.getIdentifier().get(0).getValue(),
            reportDocRef.getContext().getPeriod().getStartElement().asStringValue(),
            reportDocRef.getContext().getPeriod().getEndElement().asStringValue());

    // Create ReportContext to be used by MeasureEvaluator
    ReportContext context = new ReportContext(this.getFhirDataProvider());
    context.setReportId(measureReport.getIdElement().getIdPart());
    context.setMeasureId(measure.getIdElement().getIdPart());

    logger.debug("Re-evaluating measure with state of data on FHIR server");

    MeasureReport updatedMeasureReport = null;

    updatedMeasureReport.setId(reportId);
    updatedMeasureReport.setExtension(measureReport.getExtension());    // Copy extensions from the original report before overwriting

    // Increment the version of the report
    FhirHelper.incrementMinorVersion(reportDocRef);

    logger.debug(String.format("Updating DocumentReference and MeasureReport for report %s", reportId));

    // Create a bundle transaction to update the DocumentReference and MeasureReport
    Bundle reportUpdateBundle = new Bundle();
    reportUpdateBundle.setType(Bundle.BundleType.TRANSACTION);
    reportUpdateBundle.addEntry()
            .setRequest(
                    new Bundle.BundleEntryRequestComponent()
                            .setUrl("MeasureReport/" + updatedMeasureReport.getIdElement().getIdPart())
                            .setMethod(Bundle.HTTPVerb.PUT))
            .setResource(updatedMeasureReport);
    reportUpdateBundle.addEntry()
            .setRequest(
                    new Bundle.BundleEntryRequestComponent()
                            .setUrl("DocumentReference/" + reportDocRef.getIdElement().getIdPart())
                            .setMethod(Bundle.HTTPVerb.PUT))
            .setResource(reportDocRef);

    try {
      // Execute the update transaction bundle for MeasureReport and DocumentReference
      this.getFhirDataProvider().transaction(reportUpdateBundle);
    } catch (Exception ex) {
      logger.error("Error updating DocumentReference and MeasureReport during patient exclusion", ex);
      throw new HttpResponseException(500, "Internal Server Error");
    }

    // Record an audit event that the report has had exclusions
    this.getFhirDataProvider().audit(request, user.getJwt(), FhirHelper.AuditEventTypes.ExcludePatients, String.format("Excluded %s patients from report %s", excludedPatients.size(), reportId));

    // Create the ReportModel that will be returned
    ReportModel report = new ReportModel();
    report.setMeasureReport(updatedMeasureReport);
    report.setMeasure(measure);
    report.setIdentifier(reportId);
    report.setVersion(reportDocRef
            .getExtensionByUrl(Constants.DocumentReferenceVersionUrl) != null ?
            reportDocRef.getExtensionByUrl(Constants.DocumentReferenceVersionUrl).getValue().toString() : null);
    report.setStatus(reportDocRef.getDocStatus().toString());
    report.setDate(reportDocRef.getDate());

    return report;
  }

  public void triggerEvent(EventTypes eventType, ReportCriteria criteria, ReportContext context) {
    try {
      Method eventMethodInvoked = ApiConfigEvents.class.getMethod("get" + eventType.toString());
      List<String> classes = (List<String>) eventMethodInvoked.invoke(apiConfigEvents);
      if (classes == null) {
        logger.error(String.format("No class set-up for event %s", eventType.toString()));
        return;
      }
      for (String className : classes) {
        try {
          Class<?> clazz = Class.forName(className);
          Object myObject = clazz.newInstance();
          ((IReportGenerationEvent) myObject).execute(criteria, context);
        } catch (NoClassDefFoundError ex) {
          logger.error(String.format("Error in triggerEvent for event %s and class %s: ", eventType.toString(), className) + ex.getMessage());
        }
      }
    } catch (Exception ex) {
      logger.error(String.format("Error in triggerEvent for event  %s: ", eventType.toString()) + ex.getMessage());
    }
  }

}

