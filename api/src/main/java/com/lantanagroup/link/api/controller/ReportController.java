package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.*;
import com.lantanagroup.link.api.ReportGenerator;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.*;
import com.lantanagroup.link.model.GenerateRequest;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import com.lantanagroup.link.nhsn.ReportingPlanService;
import com.lantanagroup.link.query.QueryPhase;
import com.lantanagroup.link.query.uscore.Query;
import com.lantanagroup.link.time.StopwatchManager;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
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
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/{tenantId}/report")
public class ReportController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

  // Disallow binding of sensitive attributes
  // Ex: DISALLOWED_FIELDS = new String[]{"details.role", "details.age", "is_admin"};
  final String[] DISALLOWED_FIELDS = new String[]{};

  @Setter
  @Autowired
  private EventService eventService;

  @Autowired
  private Validator validator;

  @Autowired
  @Setter
  private ApplicationContext context;

  @Autowired
  private StopwatchManager stopwatchManager;

  @Autowired
  private SharedService sharedService;

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
      MeasureDefinition measureDefinition = this.sharedService.findMeasureDefinition(bundleId);

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

  private void queryFhir(TenantService tenantService, ReportCriteria criteria, ReportContext context, QueryPhase queryPhase) {
    if (tenantService.getConfig().getFhirQuery() == null) {
      logger.debug("Tenant {} not configured to query FHIR", tenantService.getConfig().getId());
      return;
    }

    Query query = new Query();
    query.setApplicationContext(this.context);
    query.execute(tenantService, criteria, context, queryPhase);
  }

  private List<PatientOfInterestModel> getPatientIdentifiers(TenantService tenantService, ReportCriteria criteria, ReportContext context) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    List<PatientOfInterestModel> patientOfInterestModelList;

    Class<?> patientIdResolverClass = Class.forName(this.config.getPatientIdResolver());
    IPatientIdProvider provider = (IPatientIdProvider) this.context.getBean(patientIdResolverClass);
    patientOfInterestModelList = provider.getPatientsOfInterest(tenantService, criteria, context);

    return patientOfInterestModelList;
  }

  @PostMapping("/$generate")
  public Report generateReport(
          @AuthenticationPrincipal LinkCredentials user,
          HttpServletRequest request,
          @PathVariable String tenantId,
          @RequestBody GenerateRequest input)
          throws Exception {

    if (input.getBundleIds().size() < 1) {
      throw new IllegalStateException("At least one bundleId should be specified.");
    }

    TenantService tenantService = TenantService.create(this.sharedService, tenantId);
    return generateResponse(tenantService, user, request, input.getPackageId(), input.getBundleIds(), input.getPeriodStart(), input.getPeriodEnd(), input.isRegenerate());
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
          @RequestParam String multiMeasureBundleId,
          @RequestParam String periodStart,
          @RequestParam String periodEnd,
          @PathVariable String tenantId,
          @RequestParam boolean regenerate)
          throws Exception {
    List<String> singleMeasureBundleIds;

    // should we look for multiple multimeasureid in the configuration file just in case there is a configuration mistake and error out?
    Optional<MeasurePackage> apiMeasurePackage = Optional.empty();
    for (MeasurePackage multiMeasurePackage : this.sharedService.getAllMeasurePackages()) {
      if (multiMeasurePackage.getId().equals(multiMeasureBundleId)) {
        apiMeasurePackage = Optional.of(multiMeasurePackage);
        break;
      }
    }

    // get the associated bundle-ids
    if (!apiMeasurePackage.isPresent()) {
      throw new IllegalStateException(String.format("Multimeasure %s is not set-up.", multiMeasureBundleId));
    }

    TenantService tenantService = TenantService.create(this.sharedService, tenantId);

    singleMeasureBundleIds = apiMeasurePackage.get().getMeasureIds();
    return generateResponse(tenantService, user, request, multiMeasureBundleId, singleMeasureBundleIds, periodStart, periodEnd, regenerate);
  }

  private void checkReportingPlan(TenantService tenantService, String periodStart, List<String> measureIds) throws ParseException, URISyntaxException, IOException {
    if (tenantService.getConfig().getReportingPlan() == null) {
      return;
    }

    if (!tenantService.getConfig().getReportingPlan().isEnabled()) {
      return;
    }

    if (StringUtils.isEmpty(tenantService.getConfig().getReportingPlan().getUrl())) {
      logger.error("Reporting plan for tenant {} is not configured with a URL", tenantService.getConfig().getId());
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    if (StringUtils.isEmpty(tenantService.getConfig().getCdcOrgId())) {
      logger.error("Reporting plan for tenant {} is not configured with an NHSN/CDC ORG ID", tenantService.getConfig().getId());
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    ReportingPlanService reportingPlanService = new ReportingPlanService(tenantService.getConfig().getReportingPlan().getUrl(), tenantService.getConfig().getCdcOrgId());

    logger.info("Checking MRP");
    Date date = Helper.parseFhirDate(periodStart);
    int year = date.getYear() + 1900;
    int month = date.getMonth() + 1;
    for (String bundleId : measureIds) {
      String planName = tenantService.getConfig().getReportingPlan().getPlanNames().get(bundleId);
      if (!reportingPlanService.isReporting(planName, year, month)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Measure not in MRP for specified year and month");
      }
    }
  }

  /**
   * generates a response with one or multiple reports
   */
  private Report generateResponse(TenantService tenantService, LinkCredentials user, HttpServletRequest request, String packageId, List<String> measureIds, String periodStart, String periodEnd, boolean regenerate) throws Exception {
    this.checkReportingPlan(tenantService, periodStart, measureIds);

    ReportCriteria criteria = new ReportCriteria(packageId, measureIds, periodStart, periodEnd);
    ReportContext reportContext = new ReportContext(request, user);

    this.eventService.triggerEvent(tenantService, EventTypes.BeforeMeasureResolution, criteria, reportContext);

    // Get the latest measure def and update it on the FHIR storage server
    this.resolveMeasures(criteria, reportContext);

    this.eventService.triggerEvent(tenantService, EventTypes.AfterMeasureResolution, criteria, reportContext);

    String masterIdentifierValue = ReportIdHelper.getMasterIdentifierValue(criteria);
    Report existingReport = tenantService.getReport(masterIdentifierValue);

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
      this.eventService.triggerEvent(tenantService, EventTypes.OnRegeneration, criteria, reportContext);
    }

    this.eventService.triggerEvent(tenantService, EventTypes.BeforePatientOfInterestLookup, criteria, reportContext);

    // Get the patient identifiers for the given date
    this.getPatientIdentifiers(tenantService, criteria, reportContext);

    if (reportContext.getPatientLists() == null || reportContext.getPatientLists().size() < 1) {
      String msg = "A census for the specified criteria was not found.";
      logger.error(msg);
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
    }

    this.eventService.triggerEvent(tenantService, EventTypes.AfterPatientOfInterestLookup, criteria, reportContext);

    this.eventService.triggerEvent(tenantService, EventTypes.BeforePatientDataQuery, criteria, reportContext);

    // Scoop the data for the patients and store it
    if (config.isSkipQuery()) {
      logger.info("Skipping query and store");
      for (PatientOfInterestModel patient : reportContext.getPatientsOfInterest()) {
        if (patient.getReference() != null) {
          patient.setId(patient.getReference().replaceAll("^Patient/", ""));
        }
      }
    } else {
      this.queryFhir(tenantService, criteria, reportContext, QueryPhase.INITIAL);
    }

    logger.info("Statistics after query:\n{}", this.stopwatchManager.getStatistics());
    this.eventService.triggerEvent(tenantService, EventTypes.AfterPatientDataQuery, criteria, reportContext);

    Report report = new Report();
    report.setId(masterIdentifierValue);
    report.setPeriodStart(criteria.getPeriodStart());
    report.setPeriodEnd(criteria.getPeriodEnd());
    report.setMeasureIds(measureIds);
    report.setPatientLists(reportContext.getPatientLists().stream().map(pl -> pl.getId()).collect(Collectors.toList()));

    // Preserve the version of the already-existing report
    if (existingReport != null) {
      report.setVersion(existingReport.getVersion());
    }

    logger.info("Beginning initial measure evaluation");
    this.evaluateMeasures(tenantService, criteria, reportContext, report, QueryPhase.INITIAL);

    logger.info("Beginning supplemental query and store");
    this.queryFhir(tenantService, criteria, reportContext, QueryPhase.SUPPLEMENTAL);

    logger.info("Beginning supplemental measure evaluation and aggregation");
    this.evaluateMeasures(tenantService, criteria, reportContext, report, QueryPhase.SUPPLEMENTAL);
    tenantService.saveReport(report);

    this.sharedService.audit(user, request, tenantService, AuditTypes.Generate, String.format("Generated report %s", report.getId()));
    logger.info("Done generating report {}", report.getId());

    logger.info("Statistics for entire report:\n{}", this.stopwatchManager.getStatistics());
    this.stopwatchManager.reset();

    return report;
  }

  private void evaluateMeasures(TenantService tenantService, ReportCriteria criteria, ReportContext reportContext, Report report, QueryPhase queryPhase) throws Exception {
    for (ReportContext.MeasureContext measureContext : reportContext.getMeasureContexts()) {
      if (queryPhase == QueryPhase.INITIAL) {
        measureContext.setReportId(ReportIdHelper.getMasterMeasureReportId(reportContext.getMasterIdentifierValue(), measureContext.getBundleId()));
      }

      IReportAggregator reportAggregator = (IReportAggregator) context.getBean(Class.forName(this.config.getReportAggregator()));
      ReportGenerator generator = new ReportGenerator(tenantService, this.stopwatchManager, reportContext, measureContext, criteria, this.config, reportAggregator, report);

      this.eventService.triggerEvent(tenantService, EventTypes.BeforeMeasureEval, criteria, reportContext, measureContext);

      generator.generate(queryPhase);

      this.eventService.triggerEvent(tenantService, EventTypes.AfterMeasureEval, criteria, reportContext, measureContext);
      tenantService.saveReport(report);

      if (queryPhase == QueryPhase.SUPPLEMENTAL) {
        generator.aggregate();
      }
    }
  }

  public Bundle generateBundle(TenantService tenantService, Report report) {
    FhirBundler bundler = new FhirBundler(this.eventService, tenantService, this.validator);
    logger.info("Building Bundle for MeasureReport to send...");
    List<Aggregate> aggregates = tenantService.getAggregates(report.getAggregates());
    List<MeasureReport> aggregateReports = aggregates.stream().map(Aggregate::getReport).collect(Collectors.toList());
    Bundle bundle = bundler.generateBundle(aggregateReports, report);
    logger.info(String.format("Done building Bundle for MeasureReport with %s entries", bundle.getEntry().size()));
    return bundle;
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
          @PathVariable String tenantId,
          HttpServletRequest request) throws Exception {

    if (StringUtils.isEmpty(this.config.getSender()))
      throw new IllegalStateException("Not configured for sending");

    TenantService tenantService = TenantService.create(this.sharedService, tenantId);

    if (tenantService == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    Class<?> senderClazz = Class.forName(this.config.getSender());
    IReportSender sender = (IReportSender) this.context.getBean(senderClazz);

    Report report = tenantService.getReport(reportId);

    if (report == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found");
    }

    Bundle submissionBundle = this.generateBundle(tenantService, report);
    sender.send(tenantService, submissionBundle, report, request, user);

    FhirHelper.incrementMajorVersion(report);
    report.setStatus(ReportStatuses.Submitted);

    tenantService.saveReport(report);

    this.sharedService.audit(user, request, tenantService, AuditTypes.Submit, String.format("Submitted report %s", reportId));
  }

  @GetMapping("/{reportId}/aggregate")
  public List<MeasureReport> getReportAggregates(@PathVariable String reportId, @PathVariable String tenantId) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);

    if (tenantService == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    Report report = tenantService.getReport(reportId);

    if (report == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Report %s not found", reportId));
    }

    List<Aggregate> aggregates = tenantService.getAggregates(report.getAggregates());
    return aggregates.stream().map(a -> a.getReport()).collect(Collectors.toList());
  }

  @GetMapping("/{reportId}/patientList")
  public List<PatientList> getReportPatientLists(@PathVariable String reportId, @PathVariable String tenantId) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);

    if (tenantService == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    Report report = tenantService.getReport(reportId);

    if (report == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Report %s not found", reportId));
    }

    return tenantService.getPatientLists(report.getPatientLists());
  }

  @GetMapping("/{reportId}/individual/{patientMeasureReportId}")
  public MeasureReport getPatientMeasureReport(@PathVariable String patientMeasureReportId, @PathVariable String tenantId) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);

    if (tenantService == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    PatientMeasureReport patientMeasureReport = tenantService.getPatientMeasureReport(patientMeasureReportId);

    if (patientMeasureReport == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    return patientMeasureReport.getMeasureReport();
  }
}
