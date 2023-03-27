package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.*;
import com.lantanagroup.link.api.ApiInit;
import com.lantanagroup.link.api.ReportGenerator;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.api.ApiMeasurePackage;
import com.lantanagroup.link.config.api.ApiReportDefsUrlConfig;
import com.lantanagroup.link.config.nhsn.ReportingPlanConfig;
import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.config.query.USCoreConfig;
import com.lantanagroup.link.model.*;
import com.lantanagroup.link.nhsn.ReportingPlanService;
import com.lantanagroup.link.query.IQuery;
import com.lantanagroup.link.query.QueryFactory;
import com.lantanagroup.link.time.StopwatchManager;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
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

  @Autowired
  private StopwatchManager stopwatchManager;

  @Autowired
  private ReportingPlanConfig reportingPlanConfig;

  @Autowired
  private Optional<ReportingPlanService> reportingPlanService;

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
      query.execute(criteria, context, patientsOfInterest, context.getMasterIdentifierValue(), resourceTypes, measureIds);
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
    if (reportingPlanService.isPresent()) {
      logger.info("Checking MRP");
      Date date = Helper.parseFhirDate(periodStart);
      int year = date.getYear() + 1900;
      int month = date.getMonth() + 1;
      for (String bundleId : bundleIds) {
        String planName = reportingPlanConfig.getPlanNames().get(bundleId);
        if (!reportingPlanService.get().isReporting(planName, year, month)) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Measure not in MRP for specified year and month");
        }
      }
    }
    GenerateResponse response = new GenerateResponse();
    ReportCriteria criteria = new ReportCriteria(List.of(bundleIds), periodStart, periodEnd);
    ReportContext reportContext = new ReportContext(this.getFhirDataProvider());

    reportContext.setRequest(request);
    reportContext.setUser(user);

    this.eventService.triggerEvent(EventTypes.BeforeMeasureResolution, criteria, reportContext);

    // Get the latest measure def and update it on the FHIR storage server
    this.resolveMeasures(criteria, reportContext);

    this.eventService.triggerEvent(EventTypes.AfterMeasureResolution, criteria, reportContext);

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
      this.eventService.triggerEvent(EventTypes.OnRegeneration, criteria, reportContext);
    }

    this.eventService.triggerEvent(EventTypes.BeforePatientOfInterestLookup, criteria, reportContext);

    // Get the patient identifiers for the given date
    this.getPatientIdentifiers(criteria, reportContext);

    this.eventService.triggerEvent(EventTypes.AfterPatientOfInterestLookup, criteria, reportContext);

    this.eventService.triggerEvent(EventTypes.BeforePatientDataQuery, criteria, reportContext);

    // Get the resource types to query
    Set<String> resourceTypesToQuery = new HashSet<>();
    for (ReportContext.MeasureContext measureContext : reportContext.getMeasureContexts()) {
      resourceTypesToQuery.addAll(FhirHelper.getDataRequirementTypes(measureContext.getReportDefBundle()));
    }
    // TODO: Fail if there are any data requirements that aren't listed as patient resource types?
    //       How do we expect to accurately evaluate the measure if we can't provide all of its data requirements?
    resourceTypesToQuery.retainAll(usCoreConfig.getPatientResourceTypes());

    // Scoop the data for the patients and store it
    if (config.isSkipQuery()) {
      logger.info("Skipping query and store");
      for (PatientOfInterestModel patient : reportContext.getPatientsOfInterest()) {
        if (patient.getReference() != null) {
          patient.setId(patient.getReference().replaceAll("^Patient/", ""));
        }
      }
    } else {
      this.queryAndStorePatientData(new ArrayList<>(resourceTypesToQuery), criteria, reportContext);
    }

    // TODO: Move this to just after the AfterPatientOfInterestLookup trigger
    if (reportContext.getPatientCensusLists().size() < 1 || reportContext.getPatientCensusLists() == null) {
      String msg = "A census for the specified criteria was not found.";
      logger.error(msg);
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
    }

    this.eventService.triggerEvent(EventTypes.AfterPatientDataQuery, criteria, reportContext);

    response.setMasterId(reportContext.getMasterIdentifierValue());

    this.getFhirDataProvider().audit(request, user, FhirHelper.AuditEventTypes.InitiateQuery, "Successfully Initiated Query");

    for (ReportContext.MeasureContext measureContext : reportContext.getMeasureContexts()) {

      measureContext.setReportId(ReportIdHelper.getMasterMeasureReportId(reportContext.getMasterIdentifierValue(), measureContext.getBundleId()));

      response.setMeasureHashId(ReportIdHelper.hash(measureContext.getBundleId()));

      String reportAggregatorClassName = FhirHelper.getReportAggregatorClassName(config, measureContext.getReportDefBundle());

      IReportAggregator reportAggregator = (IReportAggregator) context.getBean(Class.forName(reportAggregatorClassName));

      ReportGenerator generator = new ReportGenerator(this.stopwatchManager, reportContext, measureContext, criteria, config, user, reportAggregator);

      this.eventService.triggerEvent(EventTypes.BeforeMeasureEval, criteria, reportContext, measureContext);

      generator.generate();

      this.eventService.triggerEvent(EventTypes.AfterMeasureEval, criteria, reportContext, measureContext);

      this.eventService.triggerEvent(EventTypes.BeforeReportStore, criteria, reportContext, measureContext);

      generator.store();

      this.eventService.triggerEvent(EventTypes.AfterReportStore, criteria, reportContext, measureContext);

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

    this.getFhirDataProvider().audit(request, user, FhirHelper.AuditEventTypes.Generate, "Successfully Generated Report");
    logger.info(String.format("Done generating report %s", documentReference.getIdElement().getIdPart()));

    logger.info("Statistics:\n" + this.stopwatchManager.getStatistics());
    this.stopwatchManager.reset();

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
          @AuthenticationPrincipal LinkCredentials user,
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

    sender.send(reports, documentReference, request, user, this.getFhirDataProvider(), bundlerConfig);

    // Now that we've submitted (successfully), update the doc ref with the status and date
    this.getFhirDataProvider().updateResource(documentReference);

    String submitterName = user != null ? FhirHelper.getName(user.getPractitioner().getName()) : "Link System";

    logger.info("MeasureReport with ID " + documentReference.getMasterIdentifier().getValue() + " submitted by " + (Helper.validateLoggerValue(submitterName) ? submitterName : "") + " on " + new Date());

    this.getFhirDataProvider().audit(request, user, FhirHelper.AuditEventTypes.Send, "Successfully Sent Report");
  }
}
