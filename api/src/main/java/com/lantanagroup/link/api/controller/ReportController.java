package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.*;
import com.lantanagroup.link.api.ApiInit;
import com.lantanagroup.link.api.ReportGenerator;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.api.ApiMeasurePackage;
import com.lantanagroup.link.config.api.ApiReportDefsUrlConfig;
import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.config.query.USCoreConfig;
import com.lantanagroup.link.model.*;

import com.lantanagroup.link.nhsn.FHIRReceiver;
import com.lantanagroup.link.query.IQuery;
import com.lantanagroup.link.query.QueryFactory;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/report")
public class ReportController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(ReportController.class);
  private static final String PeriodStartParamName = "periodStart";
  private static final String PeriodEndParamName = "periodEnd";
  // Disallow binding of sensitive attributes
  // Ex: DISALLOWED_FIELDS = new String[]{"details.role", "details.age", "is_admin"};
  final String[] DISALLOWED_FIELDS = new String[]{};
  @Autowired
  private USCoreConfig usCoreConfig;

  @Setter
  @Autowired
  private EventService eventService;

  @Autowired
  @Setter
  private ApplicationContext context;

  @Autowired
  private QueryConfig queryConfig;

  @Autowired
  private ApiInit apiInit;

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.setDisallowedFields(DISALLOWED_FIELDS);
  }

  private void resolveMeasures(ReportCriteria criteria, ReportContext context) throws Exception {
    context.getMeasureContexts().clear();
    for (String bundleId : criteria.getBundleIds()) {
      // Find the report def for the given bundleId
      Bundle reportDefBundle = this.getFhirDataProvider().getBundleById(bundleId);
      if (reportDefBundle == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Did not find report def with ID " + bundleId);
      }

      ApiReportDefsUrlConfig urlConfig = config.getReportDefs().getUrlByBundleId(bundleId);
      if (urlConfig == null) {
        throw new IllegalStateException("api.report-defs.urls.url not found with bundle ID " + bundleId);
      }
      logger.info("Loading report def");
      reportDefBundle = apiInit.loadMeasureDefinition(HttpClient.newHttpClient(), urlConfig, reportDefBundle);

      // Update the context
      ReportContext.MeasureContext measureContext = new ReportContext.MeasureContext();
      measureContext.setReportDefBundle(reportDefBundle);
      measureContext.setBundleId(reportDefBundle.getIdElement().getIdPart());
      Measure measure = FhirHelper.getMeasure(reportDefBundle);
      measureContext.setMeasure(measure);
      context.getMeasureContexts().add(measureContext);
    }
  }

  private List<PatientOfInterestModel> getPatientIdentifiers(ReportCriteria criteria, ReportContext context) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    List<PatientOfInterestModel> patientOfInterestModelList;

    // TODO: When would the following condition ever be true?
    //       In the standard report generation pipeline, census lists haven't been retrieved by the time we get here
    //       Are we guarding against a case where a BeforePatientOfInterestLookup handler might have done so?
    //       (But wouldn't it be more appropriate to plug that logic in as the patient ID resolver?)
    if (context.getPatientCensusLists() != null && context.getPatientCensusLists().size() > 0) {
      patientOfInterestModelList = new ArrayList<>();
      for (ListResource censusList : context.getPatientCensusLists()) {
        for (ListResource.ListEntryComponent censusPatient : censusList.getEntry()) {
          PatientOfInterestModel patient = new PatientOfInterestModel(
                  censusPatient.getItem().getReference(),
                  IdentifierHelper.toString(censusPatient.getItem().getIdentifier()));
          patientOfInterestModelList.add(patient);
        }
      }
    } else {
      IPatientIdProvider provider;
      Class<?> patientIdResolverClass = Class.forName(this.config.getPatientIdResolver());
      Constructor<?> patientIdentifierConstructor = patientIdResolverClass.getConstructor();
      provider = (IPatientIdProvider) patientIdentifierConstructor.newInstance();
      patientOfInterestModelList = provider.getPatientsOfInterest(criteria, context, this.config);
    }

    return patientOfInterestModelList;
  }

  /**
   * Executes the configured query implementation against a list of POIs. The POI at the start of this
   * may be either identifier (such as MRN) or logical id for the FHIR Patient resource.
   *
   * @return Returns a list of the logical ids for the Patient resources stored on the internal fhir server
   * @throws Exception
   */

  private void queryAndStorePatientData(List<String> resourceTypes, ReportCriteria criteria, ReportContext context) throws Exception {
    List<PatientOfInterestModel> patientsOfInterest = context.getPatientsOfInterest();
    List<String> measureIds = context.getMeasureContexts().stream()
            .map(measureContext -> measureContext.getMeasure().getIdentifierFirstRep().getValue())
            .collect(Collectors.toList());
    try {
      // Get the data
      logger.info("Querying/scooping data for the patients: " + StringUtils.join(patientsOfInterest, ", "));
      QueryConfig queryConfig = this.context.getBean(QueryConfig.class);
      IQuery query = QueryFactory.getQueryInstance(this.context, queryConfig.getQueryClass());
      query.execute(criteria, patientsOfInterest, context.getMasterIdentifierValue(), resourceTypes, measureIds);
    } catch (Exception ex) {
      logger.error(String.format("Error scooping/storing data for the patients (%s)", StringUtils.join(patientsOfInterest, ", ")));
      throw ex;
    }
  }


  @PostMapping("/$generate")
  public GenerateResponse generateReport(
          @AuthenticationPrincipal LinkCredentials user,
          HttpServletRequest request,
          @RequestBody GenerateRequest input)
          throws Exception {

    if (input.getBundleIds().length < 1) {
      throw new IllegalStateException("At least one bundleId should be specified.");
    }
    return generateResponse(user, request, input.getBundleIds(), input.getPeriodStart(), input.getPeriodEnd(), input.isRegenerate());
  }


  /**
   * to be invoked when only a multiMeasureBundleId is provided
   *
   * @return Returns a GenerateResponse
   * @throws Exception
   */

  @PostMapping("/$generateMultiMeasure")
  public GenerateResponse generateReport(
          @AuthenticationPrincipal LinkCredentials user,
          HttpServletRequest request,
          @RequestParam("multiMeasureBundleId") String multiMeasureBundleId,
          @RequestParam("periodStart") String periodStart,
          @RequestParam("periodEnd") String periodEnd,
          boolean regenerate)
          throws Exception {
    String[] singleMeasureBundleIds = new String[]{};

    // should we look for multiple multimeasureid in the configuration file just in case there is a configuration mistake and error out?
    Optional<ApiMeasurePackage> apiMeasurePackage = Optional.empty();
    for (ApiMeasurePackage multiMeasurePackage : config.getMeasurePackages()) {
      if (multiMeasurePackage.getId().equals(multiMeasureBundleId)) {
        apiMeasurePackage = Optional.of(multiMeasurePackage);
        break;
      }
    }
    // get the associated bundle-ids
    if (!apiMeasurePackage.isPresent()) {
      throw new IllegalStateException(String.format("Multimeasure %s is not set-up.", multiMeasureBundleId));
    }
    singleMeasureBundleIds = apiMeasurePackage.get().getBundleIds();
    return generateResponse(user, request, singleMeasureBundleIds, periodStart, periodEnd, regenerate);
  }

  /**
   * generates a response with one or multiple reports
   */
  private GenerateResponse generateResponse(LinkCredentials user, HttpServletRequest request, String[] bundleIds, String periodStart, String periodEnd, boolean regenerate) throws Exception {
    GenerateResponse response = new GenerateResponse();
    ReportCriteria criteria = new ReportCriteria(List.of(bundleIds), periodStart, periodEnd);
    ReportContext reportContext = new ReportContext(this.getFhirDataProvider());

    reportContext.setRequest(request);
    reportContext.setUser(user);

    eventService.triggerEvent(EventTypes.BeforeMeasureResolution, criteria, reportContext);

    // Get the latest measure def and update it on the FHIR storage server
    this.resolveMeasures(criteria, reportContext);

    eventService.triggerEvent(EventTypes.AfterMeasureResolution, criteria, reportContext);

    String masterIdentifierValue = ReportIdHelper.getMasterIdentifierValue(criteria);

    // Search the reference document by measure criteria nd reporting period
    // searching by combination of identifiers could return multiple documents
    // like in the case one document contains the subset of identifiers of what other document contains
//    DocumentReference existingDocumentReference = this.getFhirDataProvider().findDocRefByMeasuresAndPeriod(
//            reportContext.getMeasureContexts().stream()
//                    .map(measureContext -> measureContext.getReportDefBundle().getIdentifier())
//                    .collect(Collectors.toList()),
//            periodStart,
//            periodEnd);

    // search by masterIdentifierValue to uniquely identify the document - searching by combination of identifiers could return multiple documents
    // like in the case one document contains the subset of identifiers of what other document contains
    DocumentReference existingDocumentReference = this.getFhirDataProvider().findDocRefForReport(masterIdentifierValue);
    // Search the reference document by measure criteria and reporting period
    if (existingDocumentReference != null && !regenerate) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "A report has already been generated for the specified measure and reporting period. To regenerate the report, submit your request with regenerate=true.");
    }

    if (existingDocumentReference != null) {
      existingDocumentReference = FhirHelper.incrementMinorVersion(existingDocumentReference);
    }

    // Generate the master report id
    if (!regenerate || existingDocumentReference == null) {
      // generate master report id based on the report date range and the bundles used in the report generation
      reportContext.setMasterIdentifierValue(masterIdentifierValue);
    } else {
      reportContext.setMasterIdentifierValue(existingDocumentReference.getMasterIdentifier().getValue());
      eventService.triggerEvent(EventTypes.OnRegeneration, criteria, reportContext);
    }

    eventService.triggerEvent(EventTypes.BeforePatientOfInterestLookup, criteria, reportContext);

    // Get the patient identifiers for the given date
    this.getPatientIdentifiers(criteria, reportContext);

    eventService.triggerEvent(EventTypes.AfterPatientOfInterestLookup, criteria, reportContext);

    eventService.triggerEvent(EventTypes.BeforePatientDataQuery, criteria, reportContext);

    // Get the resource types to query
    Set<String> resourceTypesToQuery = new HashSet<>();
    for (ReportContext.MeasureContext measureContext : reportContext.getMeasureContexts()) {
      resourceTypesToQuery.addAll(FhirHelper.getDataRequirementTypes(measureContext.getReportDefBundle()));
    }
    // TODO: Fail if there are any data requirements that aren't listed as patient resource types?
    //       How do we expect to accurately evaluate the measure if we can't provide all of its data requirements?
    resourceTypesToQuery.retainAll(usCoreConfig.getPatientResourceTypes());

    // Scoop the data for the patients and store it
    this.queryAndStorePatientData(new ArrayList<>(resourceTypesToQuery), criteria, reportContext);

    // TODO: Move this to just after the AfterPatientOfInterestLookup trigger
    if (reportContext.getPatientCensusLists().size() < 1 || reportContext.getPatientCensusLists() == null) {
      String msg = "A census for the specified criteria was not found.";
      logger.error(msg);
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
    }

    eventService.triggerEvent(EventTypes.AfterPatientDataQuery, criteria, reportContext);

    response.setMasterId(reportContext.getMasterIdentifierValue());

    this.getFhirDataProvider().audit(request, user.getJwt(), FhirHelper.AuditEventTypes.InitiateQuery, "Successfully Initiated Query");

    for (ReportContext.MeasureContext measureContext : reportContext.getMeasureContexts()) {

      measureContext.setReportId(ReportIdHelper.getMasterMeasureReportId(reportContext.getMasterIdentifierValue(), measureContext.getBundleId()));

      response.setMeasureHashId(ReportIdHelper.hash(measureContext.getBundleId()));

      String reportAggregatorClassName = FhirHelper.getReportAggregatorClassName(config, measureContext.getReportDefBundle());

      IReportAggregator reportAggregator = (IReportAggregator) context.getBean(Class.forName(reportAggregatorClassName));

      ReportGenerator generator = new ReportGenerator(reportContext, measureContext, criteria, config, user, reportAggregator);

      eventService.triggerEvent(EventTypes.BeforeMeasureEval, criteria, reportContext, measureContext);

      generator.generate();

      eventService.triggerEvent(EventTypes.AfterMeasureEval, criteria, reportContext, measureContext);

      eventService.triggerEvent(EventTypes.BeforeReportStore, criteria, reportContext, measureContext);

      generator.store();

      eventService.triggerEvent(EventTypes.AfterReportStore, criteria, reportContext, measureContext);

    }

    DocumentReference documentReference = this.generateDocumentReference(criteria, reportContext);

    if (existingDocumentReference != null) {
      documentReference.setId(existingDocumentReference.getId());

      Extension existingVersionExt = existingDocumentReference.getExtensionByUrl(Constants.DocumentReferenceVersionUrl);
      String existingVersion = existingVersionExt.getValue().toString();

      documentReference.getExtensionByUrl(Constants.DocumentReferenceVersionUrl).setValue(new StringType(existingVersion));

      documentReference.setContent(existingDocumentReference.getContent());
    } else {
      // generate document reference id based on the report date range and the measure used in the report generation
      UUID documentId = UUID.nameUUIDFromBytes(reportContext.getMasterIdentifierValue().getBytes(StandardCharsets.UTF_8));
      documentReference.setId(documentId.toString());
    }

    // Add the patient census list(s) to the document reference
    documentReference.getContext().getRelated().clear();
    documentReference.getContext().getRelated().addAll(reportContext.getPatientCensusLists().stream().map(censusList -> new Reference()
            .setReference("List/" + censusList.getIdElement().getIdPart())).collect(Collectors.toList()));

    this.getFhirDataProvider().updateResource(documentReference);

    this.getFhirDataProvider().audit(request, user.getJwt(), FhirHelper.AuditEventTypes.Generate, "Successfully Generated Report");

    return response;
  }

  private DocumentReference generateDocumentReference(ReportCriteria reportCriteria, ReportContext reportContext) throws ParseException {
    DocumentReference documentReference = new DocumentReference();
    Identifier identifier = new Identifier();
    identifier.setSystem(config.getDocumentReferenceSystem());
    identifier.setValue(reportContext.getMasterIdentifierValue());

    documentReference.setMasterIdentifier(identifier);
    for (ReportContext.MeasureContext measureContext : reportContext.getMeasureContexts()) {
      documentReference.addIdentifier().setSystem(Constants.MainSystem).setValue(measureContext.getBundleId());
    }

    documentReference.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);

    List<Reference> list = new ArrayList<>();
    Reference reference = new Reference();
    if (reportContext.getUser() != null && reportContext.getUser().getPractitioner() != null) {
      String practitionerId = reportContext.getUser().getPractitioner().getId();
      if (StringUtils.isNotEmpty(practitionerId)) {
        reference.setReference(practitionerId.substring(practitionerId.indexOf("Practitioner"), practitionerId.indexOf("_history") - 1));
        list.add(reference);
        documentReference.setAuthor(list);
      }
    }

    documentReference.setDocStatus(DocumentReference.ReferredDocumentStatus.PRELIMINARY);

    CodeableConcept type = new CodeableConcept();
    List<Coding> codings = new ArrayList<>();
    Coding coding = new Coding();
    coding.setCode(Constants.DocRefCode);
    coding.setSystem(Constants.LoincSystemUrl);
    coding.setDisplay(Constants.DocRefDisplay);
    codings.add(coding);
    type.setCoding(codings);
    documentReference.setType(type);

    List<DocumentReference.DocumentReferenceContentComponent> listDoc = new ArrayList<>();
    DocumentReference.DocumentReferenceContentComponent doc = new DocumentReference.DocumentReferenceContentComponent();
    Attachment attachment = new Attachment();
    attachment.setCreation(new Date());
    doc.setAttachment(attachment);
    listDoc.add(doc);
    documentReference.setContent(listDoc);

    DocumentReference.DocumentReferenceContextComponent docReference = new DocumentReference.DocumentReferenceContextComponent();
    Period period = new Period();
    Date startDate = Helper.parseFhirDate(reportCriteria.getPeriodStart());
    Date endDate = Helper.parseFhirDate(reportCriteria.getPeriodEnd());
    period.setStartElement(new DateTimeType(startDate, TemporalPrecisionEnum.MILLI, TimeZone.getDefault()));
    period.setEndElement(new DateTimeType(endDate, TemporalPrecisionEnum.MILLI, TimeZone.getDefault()));

    docReference.setPeriod(period);

    documentReference.setContext(docReference);

    documentReference.addExtension(FhirHelper.createVersionExtension("0.1"));

    return documentReference;
  }

  /**
   * Sends the specified report to the recipients configured in <strong>api.send-urls</strong>
   *
   * @param reportId - this is the report identifier after generate report was clicked
   * @param request
   * @throws Exception Thrown when the configured sender class is not found or fails to initialize or the reportId it not found
   */
  @PostMapping("/{reportId}/$send")
  public void send(
          Authentication authentication,
          @PathVariable String reportId,
          HttpServletRequest request) throws Exception {

    if (StringUtils.isEmpty(this.config.getSender()))
      throw new IllegalStateException("Not configured for sending");

    DocumentReference documentReference = this.getFhirDataProvider().findDocRefForReport(reportId);
    List<MeasureReport> reports = documentReference.getIdentifier().stream()
            .map(identifier -> ReportIdHelper.getMasterMeasureReportId(reportId, identifier.getValue()))
            .map(id -> this.getFhirDataProvider().getMeasureReportById(id))
            .collect(Collectors.toList());

    Class<?> senderClazz = Class.forName(this.config.getSender());
    IReportSender sender = (IReportSender) this.context.getBean(senderClazz);

    // update the DocumentReference's status and date
    documentReference.setDocStatus(DocumentReference.ReferredDocumentStatus.FINAL);
    documentReference.setDate(new Date());
    documentReference = FhirHelper.incrementMajorVersion(documentReference);

    sender.send(reports, documentReference, request, authentication, this.getFhirDataProvider(), bundlerConfig);

    // Now that we've submitted (successfully), update the doc ref with the status and date
    this.getFhirDataProvider().updateResource(documentReference);

    String submitterName = FhirHelper.getName(((LinkCredentials) authentication.getPrincipal()).getPractitioner().getName());

    logger.info("MeasureReport with ID " + documentReference.getMasterIdentifier().getValue() + " submitted by " + (Helper.validateLoggerValue(submitterName) ? submitterName : "") + " on " + new Date());

    this.getFhirDataProvider().audit(request, ((LinkCredentials) authentication.getPrincipal()).getJwt(), FhirHelper.AuditEventTypes.Send, "Successfully Sent Report");
  }

  @GetMapping("/{reportId}/$download/{type}")
  public void download(
          @PathVariable String reportId,
          @PathVariable String type,
          HttpServletResponse response,
          Authentication authentication,
          HttpServletRequest request) throws Exception {

    if (StringUtils.isEmpty(this.config.getDownloader()))
      throw new IllegalStateException("Not configured for downloading");

    IReportDownloader downloader;
    Class<?> downloaderClass = Class.forName(this.config.getDownloader());
    Constructor<?> downloaderCtor = downloaderClass.getConstructor();
    downloader = (IReportDownloader) downloaderCtor.newInstance();

    downloader.download(reportId, type, this.getFhirDataProvider(), response, this.ctx, this.bundlerConfig, this.eventService);

    this.getFhirDataProvider().audit(request, ((LinkCredentials) authentication.getPrincipal()).getJwt(), FhirHelper.AuditEventTypes.Export, "Successfully Exported Report for Download");
  }

  @GetMapping(value = "/{reportId}")
  public ReportModel getReport(
          @PathVariable("reportId") String reportId) throws Exception {

    ReportModel reportModel = new ReportModel();
    List<ReportModel.ReportMeasure> reportModelList = new ArrayList<>();
    reportModel.setReportMeasureList(reportModelList);
    DocumentReference documentReference = this.getFhirDataProvider().findDocRefForReport(reportId);

    for (int i = 0; i < documentReference.getIdentifier().size(); i++) {
      String encodedReport = "";
      //prevent injection from reportId parameter
      try {
        encodedReport = Helper.encodeForUrl(ReportIdHelper.getMasterMeasureReportId(reportId, documentReference.getIdentifier().get(i).getValue()));
      } catch (Exception ex) {
        logger.error(ex.getMessage());
      }

      Bundle bundle = this.getFhirDataProvider().getBundleById(documentReference.getIdentifier().get(i).getValue());
      Measure measure = FhirHelper.getMeasure(bundle);
      ReportModel.ReportMeasure reportMeasure = new ReportModel.ReportMeasure();
      // get Master Measure Report
      reportMeasure.setMeasureReport(this.getFhirDataProvider().getMeasureReportById(encodedReport));
      reportMeasure.setBundleId(bundle.getIdElement().getIdPart());
      reportMeasure.setMeasure(measure);
      reportModel.setVersion(documentReference
              .getExtensionByUrl(Constants.DocumentReferenceVersionUrl) != null ?
              documentReference.getExtensionByUrl(Constants.DocumentReferenceVersionUrl).getValue().toString() : null);
      reportModel.setStatus(documentReference.getDocStatus().toString());
      reportModel.setDate(documentReference.getDate());
      reportModelList.add(reportMeasure);
    }
    return reportModel;
  }


  @GetMapping(value = "/{reportId}/patient")
  public List<PatientReportModel> getReportPatients(
          @PathVariable("reportId") String reportId) {

    List<PatientReportModel> patientsReportModelList = new ArrayList<>();

    List<Bundle> patientBundles = getPatientBundles(reportId);

    for (Bundle patientBundle : patientBundles) {
      if (patientBundle != null && !patientBundle.getEntry().isEmpty()) {
        for (Bundle.BundleEntryComponent entry : patientBundle.getEntry()) {
          if (entry.getResource() != null && entry.getResource().getResourceType().toString().equals("Patient")) {
            Patient patient = (Patient) entry.getResource();
            PatientReportModel patientModel = FhirHelper.setPatientFields(patient, false);
            patientsReportModelList.add(patientModel);
          }
        }
      }
    }
    return patientsReportModelList;
  }


  @PutMapping(value = "/{id}")
  public void saveReport(
          @PathVariable("id") String id,
          Authentication authentication,
          HttpServletRequest request,
          @RequestBody ReportSaveModel data) throws Exception {

    DocumentReference documentReference = this.getFhirDataProvider().findDocRefForReport(id);

    documentReference = FhirHelper.incrementMinorVersion(documentReference);

    this.getFhirDataProvider().updateResource(documentReference);
    this.getFhirDataProvider().updateResource(data.getMeasureReport());

    // TODO: Wrong audit event type? We're saving the report, not sending it
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

    PatientDataModel data = new PatientDataModel();
    data.setConditions(new ArrayList<>());
    data.setMedicationRequests(new ArrayList<>());
    data.setProcedures(new ArrayList<>());
    data.setObservations(new ArrayList<>());
    data.setEncounters(new ArrayList<>());
    data.setServiceRequests(new ArrayList<>());

    List<Bundle> patientBundles = getPatientBundles(reportId, patientId);
    if (patientBundles == null || patientBundles.size() < 1) {
      return data;
    }

    for (Bundle patientBundle : patientBundles) {
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
    if (existingVersion >= 1.0f) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Report version is greater than or equal to 1.0");
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

  @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
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
          @RequestParam(required = false) String submittedDate)
          throws Exception {

    Bundle bundle;
    boolean andCond = false;
    ReportBundle reportBundle = new ReportBundle();

    String url = this.config.getDataStore().getBaseUrl();
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
   */
  @PostMapping("/{reportId}/$exclude")
  public ReportModel excludePatients(
          Authentication authentication,
          HttpServletRequest request,
          @AuthenticationPrincipal LinkCredentials user,
          @PathVariable("reportId") String reportId,
          @RequestBody List<ExcludedPatientModel> excludedPatients) throws Exception {

    DocumentReference reportDocRef = this.getFhirDataProvider().findDocRefForReport(ReportIdHelper.getMasterIdentifierValue(reportId));

    if (reportDocRef == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Report %s not found", reportId));
    }


    /*MeasureReport measureReport = this.getFhirDataProvider().getMeasureReportById(reportId);
    if (measureReport == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Report %s does not have a MeasureReport", reportId));
    }*/

    if (excludedPatients == null || excludedPatients.size() == 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No exclusions specified");
    }



    MeasureReport measureReport = this.getFhirDataProvider().getMeasureReportById(reportId);
    if (measureReport == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Report %s does not have a MeasureReport", reportId));
    }
    Measure measure = null;
    Bundle measureBundle = null;
    for (int i = 0; i < reportDocRef.getIdentifier().size(); i++) {
      String report = "";
      try {
        report = ReportIdHelper.getMasterMeasureReportId(ReportIdHelper.getMasterIdentifierValue(reportId), reportDocRef.getIdentifier().get(i).getValue());
        if(report.equals(measureReport.getIdElement().getIdPart())){
          measureBundle =  this.getFhirDataProvider().getBundleById(reportDocRef.getIdentifier().get(i).getValue());
          measure = FhirHelper.getMeasure(measureBundle);
          break;
        }
      } catch (Exception ex) {
        logger.error(ex.getMessage());
      }
    }

    List<String> bundleIds = reportDocRef.getIdentifier().stream().map(identifier -> identifier.getValue()).collect(Collectors.toList());



    //Measure measure = new Measure();
    //measure.setUrl(measureReport.getMeasure());

    if (measure == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("The measure for report %s was not found or no longer exists on the system", reportId));
    }

    Bundle excludeChangesBundle = new Bundle();
    excludeChangesBundle.setType(Bundle.BundleType.TRANSACTION);
    Boolean changedMeasureReport = false;

    for (ExcludedPatientModel excludedPatient : excludedPatients) {
      if (StringUtils.isEmpty(excludedPatient.getPatientId())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Patient ID not provided for all exclusions"));
      }

      if (excludedPatient.getReason() == null || excludedPatient.getReason().isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Excluded patient ID %s does not specify a reason", excludedPatient.getPatientId()));
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
      //Commented out until CQF ruler is fixed
      /*if (foundEvaluatedPatient.size() == 0 && !foundExcluded) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Patient %s is not included in report %s", excludedPatient.getPatientId(), reportId));
      }*/

      // Create an extension for the excluded patient on the MeasureReport
      //Commented out until CQF ruler is fixed
      /*if (!foundExcluded) {
        Extension newExtension = new Extension(Constants.ExcludedPatientExtUrl);
        newExtension.addExtension("patient", new Reference("Patient/" + excludedPatient.getPatientId()));
        newExtension.addExtension("reason", excludedPatient.getReason());
        measureReport.addExtension(newExtension);
        changedMeasureReport = true;

        // Remove the patient from evaluatedResources, or HAPI will throw a referential integrity exception since it's getting (or has been) deleted
        if (foundEvaluatedPatient.size() > 0) {
          foundEvaluatedPatient.forEach(ep -> measureReport.getEvaluatedResource().remove(ep));
        }
      }*/

      logger.debug(String.format("Checking if patient %s has been deleted already", excludedPatient.getPatientId()));

      try {
        // Try to GET the patient to see if it has already been deleted or not
        this.getFhirDataProvider().tryGetResource("Patient", excludedPatient.getPatientId());
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

      this.getFhirDataProvider().transaction(excludeChangesBundle);
    }

    // Create ReportCriteria to be used by MeasureEvaluator
    ReportCriteria criteria = new ReportCriteria(
            bundleIds,
            reportDocRef.getContext().getPeriod().getStartElement().asStringValue(),
            reportDocRef.getContext().getPeriod().getEndElement().asStringValue());

    // Create ReportContext to be used by MeasureEvaluator
    ReportContext reportContext = new ReportContext(this.getFhirDataProvider());
    reportContext.setRequest(request);
    reportContext.setUser(user);
    reportContext.setMasterIdentifierValue(reportId);
    ReportContext.MeasureContext measureContext = new ReportContext.MeasureContext();
    //  measureContext.setBundleId(reportDefIdentifier);
    // TODO: Set report def bundle on measure context
    measureContext.setMeasure(measure);
    measureContext.setReportId(measureReport.getIdElement().getIdPart());
    reportContext.getMeasureContexts().add(measureContext);

    logger.debug("Re-evaluating measure with state of data on FHIR server");

    List<String> excludedPatientReportIds = excludedPatients.stream().map(
            patient -> ReportIdHelper.getPatientMeasureReportId(reportId, patient.getPatientId())).collect(Collectors.toList());

    Set<String> reportRefs = measureReport.getContained().stream().map(list -> (ListResource) list).flatMap(list -> list.getEntry().stream().map(
            entry -> entry.getItem().getReference().substring(entry.getItem().getReference().indexOf("/") + 1))).collect(Collectors.toSet());

    measureContext.setPatientReports(reportRefs.stream().filter(ref -> !excludedPatientReportIds.contains(ref)).map(ref -> {
      MeasureReport patientReport = this.getFhirDataProvider().getMeasureReportById(ref); return patientReport;
    }).collect(Collectors.toList()));

    String reportAggregatorClassName = FhirHelper.getReportAggregatorClassName(config, measureBundle);
    IReportAggregator reportAggregator = (IReportAggregator) context.getBean(Class.forName(reportAggregatorClassName));
    MeasureReport updatedMeasureReport = reportAggregator.generate(criteria, reportContext, measureContext);

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

    // Execute the update transaction bundle for MeasureReport and DocumentReference
    this.getFhirDataProvider().transaction(reportUpdateBundle);

    // Record an audit event that the report has had exclusions
    this.getFhirDataProvider().audit(request, user.getJwt(), FhirHelper.AuditEventTypes.ExcludePatients, String.format("Excluded %s patients from report %s", excludedPatients.size(), reportId));

    // Create the ReportModel that will be returned
    ReportModel report = new ReportModel();
//    report.setMeasureReport(updatedMeasureReport);
//    report.setMeasure(measure);
//    report.setIdentifier(reportId);
    report.setVersion(reportDocRef
            .getExtensionByUrl(Constants.DocumentReferenceVersionUrl) != null ?
            reportDocRef.getExtensionByUrl(Constants.DocumentReferenceVersionUrl).getValue().toString() : null);
    report.setStatus(reportDocRef.getDocStatus().toString());
    report.setDate(reportDocRef.getDate());

    return report;
  }

  /**
   * Retrieves patient data bundles either from a submission bundle if its report had been sent
   * or from the master measure report if it had not.
   * Can also search for specific patient data bundles by patientId or patientReportId
   *
   * @param masterReportId master report id needed to get the master reportId
   * @param patientId      if searching for a specific patient's data by patientId
   * @return a list of bundles containing data for each patient
   */

  private List<Bundle> getPatientBundles(String masterReportId, String patientId) {
    List<Bundle> patientBundles = new ArrayList<>();

    logger.info("Report not sent: Searching for patient data from master measure report");
    MeasureReport masterReport = this.getFhirDataProvider().getMeasureReportById(masterReportId);
    ListResource refs = (ListResource) masterReport.getContained().get(0);
    for (ListResource.ListEntryComponent ref : refs.getEntry()) {
      String[] refParts = ref.getItem().getReference().split("/");
      if (refParts.length > 1) {
        if (patientId != null && !patientId.equals("")) {
          logger.info("Searching for specified report " + patientId.hashCode() + " checking if part of " + refParts[refParts.length - 1]);
          if (refParts[refParts.length - 1].contains(String.valueOf(patientId.hashCode()))) {
            logger.info("Searching for specified patient " + (Helper.validateLoggerValue(patientId) ? patientId : ""));
            IBaseResource patientBundle = this.getFhirDataProvider().getBundleById(ReportIdHelper.getPatientDataBundleId(refParts[refParts.length - 1]));
            patientBundles.add((Bundle) patientBundle);
            break;
          }
        } else {
          logger.info("Searching for patient report " + refParts[refParts.length - 1] + " out of all patients in master measure report");
          IBaseResource patientBundle = this.getFhirDataProvider().getBundleById(ReportIdHelper.getPatientDataBundleId(refParts[refParts.length - 1]));
          patientBundles.add((Bundle) patientBundle);
        }
      }
    }
    return patientBundles;
  }

  /**
   * calls getPatientBundles without having to provide a specified patientReportId or patientId
   *
   * @param reportID master report id needed to get the master reportId
   * @return a list of bundles containing data for each patient
   */
  private List<Bundle> getPatientBundles(String reportID) {
    return getPatientBundles(reportID, "");
  }

  private Bundle getPatientResourcesById(String patientId, Bundle ResourceBundle) {
    Bundle patientResourceBundle = new Bundle();
    for (Bundle.BundleEntryComponent entry : ResourceBundle.getEntry()) {
      if (entry.getResource().getId().contains(patientId)) {
        patientResourceBundle.addEntry(entry);
      } else {
        String type = entry.getResource().getResourceType().toString();
        if (type.equals("Condition")) {
          Condition condition = (Condition) entry.getResource();
          if (condition.getSubject().getReference().equals("Patient/" + patientId)) {
            patientResourceBundle.addEntry(entry);
          }
        }
        if (type.equals("MedicationRequest")) {
          MedicationRequest medicationRequest = (MedicationRequest) entry.getResource();
          if (medicationRequest.getSubject().getReference().equals("Patient/" + patientId)) {
            patientResourceBundle.addEntry(entry);
          }
        }
        if (type.equals("Observation")) {
          Observation observation = (Observation) entry.getResource();
          if (observation.getSubject().getReference().equals("Patient/" + patientId)) {
            patientResourceBundle.addEntry(entry);
          }
        }
        if (type.equals("Procedure")) {
          Procedure procedure = (Procedure) entry.getResource();
          if (procedure.getSubject().getReference().equals("Patient/" + patientId)) {
            patientResourceBundle.addEntry(entry);
          }
        }
        if (type.equals("Encounter")) {
          Encounter encounter = (Encounter) entry.getResource();
          if (encounter.getSubject().getReference().equals("Patient/" + patientId)) {
            patientResourceBundle.addEntry(entry);
          }
        }
        if (type.equals("ServiceRequest")) {
          ServiceRequest serviceRequest = (ServiceRequest) entry.getResource();
          if (serviceRequest.getSubject().getReference().equals("Patient/" + patientId)) {
            patientResourceBundle.addEntry(entry);
          }
        }
      }
    }
    return patientResourceBundle;
  }

  private Bundle getPatientBundleByReport(MeasureReport patientReport) {
    Bundle patientBundle = new Bundle();
    List<Reference> refs = patientReport.getEvaluatedResource();
    for (Reference ref : refs) {
      String[] refParts = ref.getReference().split("/");
      if (refParts.length == 2) {
        Resource resource = (Resource) this.getFhirDataProvider().tryGetResource(refParts[0], refParts[1]);
        Bundle.BundleEntryComponent component = new Bundle.BundleEntryComponent().setResource(resource);
        patientBundle.addEntry(component);
      }
    }
    return patientBundle;
  }

}
