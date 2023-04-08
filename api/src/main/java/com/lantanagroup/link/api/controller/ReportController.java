package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.*;
import com.lantanagroup.link.api.ReportGenerator;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.api.ApiMeasurePackage;
import com.lantanagroup.link.config.nhsn.ReportingPlanConfig;
import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.config.query.USCoreConfig;
import com.lantanagroup.link.db.MongoService;
import com.lantanagroup.link.db.model.*;
import com.lantanagroup.link.model.GenerateRequest;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import com.lantanagroup.link.nhsn.ReportingPlanService;
import com.lantanagroup.link.query.IQuery;
import com.lantanagroup.link.query.QueryFactory;
import com.lantanagroup.link.time.StopwatchManager;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
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
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/report")
public class ReportController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

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
  private StopwatchManager stopwatchManager;

  @Autowired
  private ReportingPlanConfig reportingPlanConfig;

  @Autowired
  private Optional<ReportingPlanService> reportingPlanService;

  @Autowired
  private MongoService mongoService;

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.setDisallowedFields(DISALLOWED_FIELDS);
  }

  /**
   * Increments the minor version of the specified report
   *
   * @param report
   */
  public static void incrementMinorVersion(Report report) {
    if (StringUtils.isEmpty(report.getVersion())) {
      report.setVersion("0.1");
    } else {
      String version = report.getVersion();
      report.setVersion(version.substring(0, version.indexOf(".") + 1) + (Integer.parseInt(version.substring(version.indexOf(".") + 1)) + 1));
    }
  }

  private void resolveMeasures(ReportCriteria criteria, ReportContext context) throws Exception {
    context.getMeasureContexts().clear();
    for (String bundleId : criteria.getBundleIds()) {
      MeasureDefinition measureDefinition = this.mongoService.findMeasureDefinition(bundleId);

      if (measureDefinition == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Did not find measure def with ID " + bundleId);
      }

      // Update the context
      ReportContext.MeasureContext measureContext = new ReportContext.MeasureContext();
      measureContext.setReportDefBundle(measureDefinition.getBundle());
      measureContext.setBundleId(measureDefinition.getMeasureId());

      Measure measure = FhirHelper.getMeasure(measureDefinition.getBundle());
      measureContext.setMeasure(measure);

      context.getMeasureContexts().add(measureContext);
    }
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
      query.execute(criteria, context, resourceTypes, measureIds);
    } catch (Exception ex) {
      logger.error(String.format("Error scooping/storing data for the patients (%s)", StringUtils.join(patientsOfInterest, ", ")));
      throw ex;
    }
  }

  private List<PatientOfInterestModel> getPatientIdentifiers(ReportCriteria criteria, ReportContext context) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    List<PatientOfInterestModel> patientOfInterestModelList;

    Class<?> patientIdResolverClass = Class.forName(this.config.getPatientIdResolver());
    IPatientIdProvider provider = (IPatientIdProvider) this.context.getBean(patientIdResolverClass);
    patientOfInterestModelList = provider.getPatientsOfInterest(criteria, context);

    return patientOfInterestModelList;
  }

  @PostMapping("/$generate")
  public Report generateReport(
          @AuthenticationPrincipal LinkCredentials user,
          HttpServletRequest request,
          @RequestBody GenerateRequest input)
          throws Exception {

    if (input.getBundleIds().length < 1) {
      throw new IllegalStateException("At least one bundleId should be specified.");
    }
    return generateResponse(user, request, input.getPackageId(), input.getBundleIds(), input.getPeriodStart(), input.getPeriodEnd(), input.isRegenerate());
  }

  /**
   * to be invoked when only a multiMeasureBundleId is provided
   *
   * @return Returns a GenerateResponse
   * @throws Exception
   */
  @PostMapping("/$generateMultiMeasure")
  public Report generateReport(
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
    return generateResponse(user, request, multiMeasureBundleId, singleMeasureBundleIds, periodStart, periodEnd, regenerate);
  }

  /**
   * generates a response with one or multiple reports
   */
  private Report generateResponse(LinkCredentials user, HttpServletRequest request, String packageId, String[] bundleIds, String periodStart, String periodEnd, boolean regenerate) throws Exception {
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

    ReportCriteria criteria = new ReportCriteria(packageId, List.of(bundleIds), periodStart, periodEnd);
    ReportContext reportContext = new ReportContext(request, user);

    this.eventService.triggerEvent(EventTypes.BeforeMeasureResolution, criteria, reportContext);

    // Get the latest measure def and update it on the FHIR storage server
    this.resolveMeasures(criteria, reportContext);

    this.eventService.triggerEvent(EventTypes.AfterMeasureResolution, criteria, reportContext);

    String masterIdentifierValue = ReportIdHelper.getMasterIdentifierValue(criteria);
    Report existingReport = this.mongoService.getReport(masterIdentifierValue);

    // Search the reference document by measure criteria and reporting period
    if (existingReport != null && !regenerate) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "A report has already been generated for the specified measure and reporting period. To regenerate the report, submit your request with regenerate=true.");
    }

    if (existingReport != null) {
      incrementMinorVersion(existingReport);
    }

    // Generate the master report id
    if (!regenerate || existingReport == null) {
      // generate master report id based on the report date range and the bundles used in the report generation
      reportContext.setMasterIdentifierValue(masterIdentifierValue);
    } else {
      reportContext.setMasterIdentifierValue(existingReport.getId());
      this.eventService.triggerEvent(EventTypes.OnRegeneration, criteria, reportContext);
    }

    this.eventService.triggerEvent(EventTypes.BeforePatientOfInterestLookup, criteria, reportContext);

    // Get the patient identifiers for the given date
    this.getPatientIdentifiers(criteria, reportContext);

    if (reportContext.getPatientLists() == null || reportContext.getPatientLists().size() < 1) {
      String msg = "A census for the specified criteria was not found.";
      logger.error(msg);
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
    }

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

    this.eventService.triggerEvent(EventTypes.AfterPatientDataQuery, criteria, reportContext);

    Report report = new Report();
    report.setId(masterIdentifierValue);
    report.setPeriodStart(criteria.getPeriodStart());
    report.setPeriodEnd(criteria.getPeriodEnd());
    report.setMeasureIds(Arrays.asList(bundleIds));
    report.setPatientLists(reportContext.getPatientLists().stream().map(pl -> pl.getId()).collect(Collectors.toList()));

    // Preserve the version of the already-existing report
    if (existingReport != null) {
      report.setVersion(existingReport.getVersion());
    }

    for (ReportContext.MeasureContext measureContext : reportContext.getMeasureContexts()) {

      measureContext.setReportId(ReportIdHelper.getMasterMeasureReportId(reportContext.getMasterIdentifierValue(), measureContext.getBundleId()));

      String reportAggregatorClassName = FhirHelper.getReportAggregatorClassName(config, measureContext.getReportDefBundle());
      IReportAggregator reportAggregator = (IReportAggregator) context.getBean(Class.forName(reportAggregatorClassName));

      ReportGenerator generator = new ReportGenerator(this.mongoService, this.stopwatchManager, reportContext, measureContext, criteria, this.config, reportAggregator, report);

      this.eventService.triggerEvent(EventTypes.BeforeMeasureEval, criteria, reportContext, measureContext);

      generator.generate();

      this.eventService.triggerEvent(EventTypes.AfterMeasureEval, criteria, reportContext, measureContext);
    }

    this.mongoService.saveReport(report);

    this.mongoService.audit(user, request, AuditTypes.Generate, String.format("Generated report %s", report.getId()));
    logger.info("Done generating report {}", report.getId());

    logger.info("Statistics:\n{}", this.stopwatchManager.getStatistics());
    this.stopwatchManager.reset();

    return report;
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

    Class<?> senderClazz = Class.forName(this.config.getSender());
    IReportSender sender = (IReportSender) this.context.getBean(senderClazz);

    Report report = this.mongoService.getReport(reportId);

    sender.send(report, request, user);

    FhirHelper.incrementMajorVersion(report);
    report.setStatus(ReportStatuses.Submitted);

    this.mongoService.saveReport(report);

    this.mongoService.audit(user, request, AuditTypes.Submit, String.format("Submitted report %s", reportId));
  }

  @GetMapping("/{reportId}/aggregate")
  public List<MeasureReport> getAggregates(@PathVariable String reportId) {
    Report report = this.mongoService.getReport(reportId);
    List<Aggregate> aggregates = this.mongoService.getAggregates(report.getAggregates());
    return aggregates.stream().map(a -> a.getReport()).collect(Collectors.toList());
  }

  @GetMapping("/{reportId}/patientList")
  public List<PatientList> getReportPatientLists(@PathVariable String reportId) {
    Report report = this.mongoService.getReport(reportId);
    return this.mongoService.getPatientLists(report.getPatientLists());
  }

  @GetMapping("/{reportId}/individual/{patientMeasureReportId}")
  public MeasureReport getPatientMeasureReport(@PathVariable String patientMeasureReportId) {
    PatientMeasureReport patientMeasureReport = this.mongoService.getPatientMeasureReport(patientMeasureReportId);

    if (patientMeasureReport == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    return patientMeasureReport.getMeasureReport();
  }
}
