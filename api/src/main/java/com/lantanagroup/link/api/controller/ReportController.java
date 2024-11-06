package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.EncodingEnum;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.lantanagroup.link.*;
import com.lantanagroup.link.api.ReportGenerator;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.*;
import com.lantanagroup.link.db.model.tenant.FhirQuery;
import com.lantanagroup.link.db.model.tenant.QueryPlan;
import com.lantanagroup.link.db.model.tenant.Tenant;
import com.lantanagroup.link.model.GenerateRequest;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import com.lantanagroup.link.nhsn.ReportingPlanService;
import com.lantanagroup.link.query.QueryPhase;
import com.lantanagroup.link.query.uscore.Query;
import com.lantanagroup.link.time.Stopwatch;
import com.lantanagroup.link.time.StopwatchManager;
import com.lantanagroup.link.validation.ValidationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/{tenantId}/report")
public class ReportController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(ReportController.class);
  private static final Map<String, Lock> tenantLocks = new ConcurrentHashMap<>();

  // Disallow binding of sensitive attributes
  // Ex: DISALLOWED_FIELDS = new String[]{"details.role", "details.age", "is_admin"};
  final String[] DISALLOWED_FIELDS = new String[]{};

  @Setter
  @Autowired
  private EventService eventService;

  @Autowired
  @Setter
  private ApplicationContext context;

  @Autowired
  private StopwatchManager stopwatchManager;

  @Autowired
  private SharedService sharedService;

  @Autowired
  private ValidationService validationService;

  @Autowired
  private ApiConfig apiConfig;


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
      MeasureDefinition measureDefinition = this.sharedService.getMeasureDefinition(bundleId);

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
    try (Stopwatch stopwatch = stopwatchManager.start(queryPhase.toString(), Constants.CATEGORY_QUERY)) {
      query.execute(tenantService, criteria, context, queryPhase);
    }
  }

  private void loadPatientIdentifiers(TenantService tenantService, ReportCriteria criteria, ReportContext context) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    Class<?> patientIdResolverClass = Class.forName(this.config.getPatientIdResolver());
    IPatientIdProvider provider = (IPatientIdProvider) this.context.getBean(patientIdResolverClass);
    provider.loadPatientsOfInterest(tenantService, criteria, context);
  }

  @PostMapping("/{reportId}/$regenerate")
  public Report regenerateReport(
          @AuthenticationPrincipal LinkCredentials user,
          HttpServletRequest request,
          @PathVariable String tenantId,
          @PathVariable String reportId,
          @RequestBody GenerateRequest input)
          throws Exception {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);

    if (tenantService == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    Report report = tenantService.getReport(reportId);

    if (report == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found");
    }

    GenerateRequest generateRequest = new GenerateRequest();
    generateRequest.setBundleIds(report.getMeasureIds());
    generateRequest.setPeriodStart(report.getPeriodStart());
    generateRequest.setPeriodEnd(report.getPeriodEnd());
    generateRequest.setRegenerate(true);
    generateRequest.setValidate(input.isValidate());
    generateRequest.setSkipQuery(input.isSkipQuery());
    generateRequest.setDebugPatients(input.getDebugPatients());

    return generateReport(user, request, tenantId, generateRequest);
  }

  @GetMapping("/{reportId}/$regenerate")
  public Report regenerateReport(
          @AuthenticationPrincipal LinkCredentials user,
          HttpServletRequest request,
          @PathVariable String tenantId,
          @PathVariable String reportId,
          @RequestParam(defaultValue = "false") boolean validate,
          @RequestParam(defaultValue = "false") boolean skipQuery,
          @RequestParam(required = false, defaultValue = "") List<String> debugPatients)
          throws Exception {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);

    if (tenantService == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    Report report = tenantService.getReport(reportId);

    if (report == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found");
    }

    GenerateRequest generateRequest = new GenerateRequest();
    generateRequest.setBundleIds(report.getMeasureIds());
    generateRequest.setPeriodStart(report.getPeriodStart());
    generateRequest.setPeriodEnd(report.getPeriodEnd());
    generateRequest.setRegenerate(true);
    generateRequest.setValidate(validate);
    generateRequest.setSkipQuery(skipQuery);
    generateRequest.setDebugPatients(debugPatients);

    return generateReport(user, request, tenantId, generateRequest);
  }

  @PostMapping("/$generate")
  public Report generateReport(
          @AuthenticationPrincipal LinkCredentials user,
          HttpServletRequest request,
          @PathVariable String tenantId,
          @RequestBody GenerateRequest input)
          throws Exception {

    if (input.getBundleIds().isEmpty()) {
      throw new IllegalStateException("At least one bundleId should be specified.");
    }

    TenantService tenantService = TenantService.create(this.sharedService, tenantId);
    return generateResponse(tenantService, user, request, input.getPackageId(), input.getBundleIds(), input.getPeriodStart(), input.getPeriodEnd(), input.isRegenerate(), input.isValidate(), input.isSkipQuery(), input.getDebugPatients());
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
          @RequestParam boolean regenerate,
          @RequestParam(defaultValue = "false") boolean validate,
          @RequestParam(defaultValue = "false") boolean skipQuery,
          @RequestParam(required = false, defaultValue = "") List<String> debugPatients)
          throws Exception {
    List<String> singleMeasureBundleIds;

    // should we look for multiple multimeasureid in the configuration file just in case there is a configuration mistake and error out?
    Optional<MeasurePackage> apiMeasurePackage = Optional.empty();
    for (MeasurePackage multiMeasurePackage : this.sharedService.getMeasurePackages()) {
      if (multiMeasurePackage.getId().equals(multiMeasureBundleId)) {
        apiMeasurePackage = Optional.of(multiMeasurePackage);
        break;
      }
    }

    Optional<List<String>> debugPatientList = Optional.of(debugPatients);

    // get the associated bundle-ids
    if (!apiMeasurePackage.isPresent()) {
      throw new IllegalStateException(String.format("Multimeasure %s is not set-up.", multiMeasureBundleId));
    }

    TenantService tenantService = TenantService.create(this.sharedService, tenantId);

    singleMeasureBundleIds = apiMeasurePackage.get().getMeasureIds();
    return generateResponse(tenantService, user, request, multiMeasureBundleId, singleMeasureBundleIds, periodStart, periodEnd, regenerate, validate, skipQuery, debugPatients);
  }

  private void checkReportingPlan(TenantService tenantService, String periodStart, List<String> measureIds) throws ParseException, URISyntaxException, IOException {
    if (apiConfig.getReportingPlan() == null) {
      return;
    }

    if (!apiConfig.getReportingPlan().isEnabled()) {
      return;
    }

    if (StringUtils.isEmpty(apiConfig.getReportingPlan().getUrl())) {
      logger.error("Reporting plan for tenant {} is not configured with a URL", tenantService.getConfig().getId());
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    if (StringUtils.isEmpty(tenantService.getConfig().getCdcOrgId())) {
      logger.error("Reporting plan for tenant {} is not configured with an NHSN/CDC ORG ID", tenantService.getConfig().getId());
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    ReportingPlanService reportingPlanService = new ReportingPlanService(apiConfig.getReportingPlan(), tenantService.getConfig().getCdcOrgId());

    Date date = Helper.parseFhirDate(periodStart);
    int year = date.getYear() + 1900;
    int month = date.getMonth() + 1;
    for (String bundleId : measureIds) {
      String planName = apiConfig.getReportingPlan().getPlanNames().get(bundleId);
      if (!reportingPlanService.isReporting(planName, year, month)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Measure not in MRP for specified year and month");
      }
    }
  }

  /**
   * generates a response with one or multiple reports
   */
  private Report generateResponse(TenantService tenantService, LinkCredentials user, HttpServletRequest request, String packageId, List<String> measureIds, String periodStart, String periodEnd, boolean regenerate, boolean validate, boolean skipQuery, List<String>  debugPatients) throws Exception {
    Report report = null;
    Lock tenantLock = tenantLocks.computeIfAbsent(tenantService.getConfig().getId(), id -> new ReentrantLock());
    if (!tenantLock.tryLock()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Report in progress for tenant");
    }
    try(var stopwatch = stopwatchManager.start(Constants.REPORT_GENERATION_TASK, Constants.CATEGORY_REPORT)) {
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
      this.loadPatientIdentifiers(tenantService, criteria, reportContext);

      if (reportContext.getPatientLists() == null || reportContext.getPatientLists().isEmpty()) {
        String msg = "A census for the specified criteria was not found.";
        logger.error(msg);
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
      }

      this.eventService.triggerEvent(tenantService, EventTypes.AfterPatientOfInterestLookup, criteria, reportContext);

      QueryPlan queryPlan = this.getQueryPlan(tenantService.getConfig(), criteria.getQueryPlanId());
      if (queryPlan == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query plan not found: " + criteria.getQueryPlanId());
      }
      reportContext.setQueryPlan(queryPlan);

      // Validate the debug patients against the patient of interest list
      if(!debugPatients.isEmpty() && !debugPatients.contains("*")) {
        List<String> invalidDebugPatientsList = debugPatients.stream().filter(dp -> !reportContext.getPatientsOfInterest().stream().map(PatientOfInterestModel::getReference).collect(Collectors.toList()).contains(dp)).collect(Collectors.toList());
        if (!invalidDebugPatientsList.isEmpty()) {
          String msg = String.format("Debugging patients: %s do not exist", invalidDebugPatientsList);
          logger.error(msg);
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
        }
      }
      reportContext.setDebugPatients(debugPatients);

      report = new Report();
      report.setId(masterIdentifierValue);
      report.setPeriodStart(criteria.getPeriodStart());
      report.setPeriodEnd(criteria.getPeriodEnd());
      report.setMeasureIds(measureIds);
      report.setDeviceInfo(FhirHelper.getDevice(this.config, tenantService));
      report.setQueryPlan(new YAMLMapper().writeValueAsString(queryPlan));

      // Preserve the version of the already-existing report
      if (existingReport != null) {
        report.setVersion(existingReport.getVersion());
      }

      MDC.put("report", String.format("%s-%s-%s",
              tenantService.getConfig().getId(), report.getId(), report.getVersion()));

      tenantService.saveReport(report, reportContext.getPatientLists());

      this.eventService.triggerEvent(tenantService, EventTypes.BeforePatientDataQuery, criteria, reportContext);

      // Scoop the data for the patients and store it
      if (config.isSkipQuery() || skipQuery) {
        logger.info("Skipping initial query and store");
        for (PatientOfInterestModel patient : reportContext.getPatientsOfInterest()) {
          if (patient.getReference() != null) {
            patient.setId(patient.getReference().replaceAll("^Patient/", ""));
          }
        }
      } else {
        logger.info("Beginning initial query and store");
        tenantService.beginReport(masterIdentifierValue);
        this.queryFhir(tenantService, criteria, reportContext, QueryPhase.INITIAL);
      }

      this.eventService.triggerEvent(tenantService, EventTypes.AfterPatientDataQuery, criteria, reportContext);

      logger.info("Beginning initial measure evaluation");
      this.evaluateMeasures(tenantService, criteria, reportContext, report, QueryPhase.INITIAL, false);

      if (config.isSkipQuery() || skipQuery || CollectionUtils.isEmpty(reportContext.getQueryPlan().getSupplemental())) {
        logger.info("Skipping supplemental query and store");
        logger.info("Beginning aggregation");
        this.evaluateMeasures(tenantService, criteria, reportContext, report, QueryPhase.SUPPLEMENTAL, true);
      } else {
        logger.info("Beginning supplemental query and store");
        this.queryFhir(tenantService, criteria, reportContext, QueryPhase.SUPPLEMENTAL);
        logger.info("Beginning supplemental measure evaluation and aggregation");
        this.evaluateMeasures(tenantService, criteria, reportContext, report, QueryPhase.SUPPLEMENTAL, false);
      }

      report.setGeneratedTime(new Date());
      tenantService.saveReport(report);

      this.sharedService.audit(user, request, tenantService, AuditTypes.Generate, String.format("Generated report %s", report.getId()));
      logger.info("Done generating report {}, continuing to bundle and validate...", report.getId());

      if (validate) {
        try {
          this.validationService.validate(stopwatchManager, tenantService, report);
          logger.info("Done validating report");
        } catch (Exception ex) {
          logger.error("Error validating report {}", report.getId(), ex);
        }
      } else {
        logger.info("Skipping validation for report {}", report.getId());
      }
    } finally {
      tenantLock.unlock();
      MDC.clear();
    }

    this.stopwatchManager.storeMetrics(tenantService.getConfig().getId(), report.getId(), report.getVersion());
    logger.info("Statistics for report {} are:\n{}", report.getId(), this.stopwatchManager.getStatistics());
    this.stopwatchManager.reset();

    return report;
  }

  private QueryPlan getQueryPlan(Tenant tenant, String queryPlanId) {
    FhirQuery fhirQuery = tenant.getFhirQuery();
    if (fhirQuery == null) {
      return null;
    }

    // Attempt to retrieve from URL
    String url = fhirQuery.getQueryPlanUrls().get(queryPlanId);
    String content = null;
    if (url != null) {
      logger.info("Retrieving query plan: {}", url);
      try {
        content = IOUtils.toString(new URL(url), StandardCharsets.UTF_8);
      } catch (IOException e) {
        logger.error("Failed to retrieve query plan", e);
      }
    }

    if (content != null) {
      YAMLMapper mapper = new YAMLMapper();

      // Attempt to parse as FHIR Library
      try {
        EncodingEnum encoding = EncodingEnum.detectEncoding(content);
        IParser parser = encoding.newParser(FhirContextProvider.getFhirContext());
        Library library = parser.parseResource(Library.class, content);
        for (int index = 0; index < library.getContent().size(); index++) {
          byte[] data = library.getContent().get(index).getData();
          try {
            return mapper.readValue(data, QueryPlan.class);
          } catch (Exception e) {
            logger.warn("Failed to parse content at index {}", index);
          }
        }
      } catch (Exception e) {
        logger.warn("Failed to parse as FHIR Library");
      }

      // Attempt to parse as raw YAML
      try {
        return mapper.readValue(content, QueryPlan.class);
      } catch (Exception e) {
        logger.warn("Failed to parse as raw YAML");
      }
    }

    // Fall back to explicitly provided query plan
    return fhirQuery.getQueryPlans().get(queryPlanId);
  }

  private void evaluateMeasures(TenantService tenantService, ReportCriteria criteria, ReportContext reportContext, Report report, QueryPhase queryPhase, boolean aggregateOnly) throws Exception {
    for (ReportContext.MeasureContext measureContext : reportContext.getMeasureContexts()) {
      if (queryPhase == QueryPhase.INITIAL) {
        measureContext.setReportId(ReportIdHelper.getMasterMeasureReportId(reportContext.getMasterIdentifierValue(), measureContext.getBundleId()));
      }

      IReportAggregator reportAggregator = (IReportAggregator) context.getBean(Class.forName(this.config.getReportAggregator()));
      ReportGenerator generator = new ReportGenerator(sharedService, tenantService, this.stopwatchManager, reportContext, measureContext, criteria, this.config, reportAggregator, report);

      if (!aggregateOnly) {
        this.eventService.triggerEvent(tenantService, EventTypes.BeforeMeasureEval, criteria, reportContext, measureContext);

        try (Stopwatch stopwatch = stopwatchManager.start(queryPhase.toString(), Constants.CATEGORY_EVALUATE)) {
          generator.generate(queryPhase);
        }

        this.eventService.triggerEvent(tenantService, EventTypes.AfterMeasureEval, criteria, reportContext, measureContext);
      }

      if (queryPhase == QueryPhase.SUPPLEMENTAL) {
        generator.aggregate();
      }
    }
  }

  /**
   * Sends the specified report to the recipients configured in <strong>api.send-urls</strong>
   *
   * @param reportId - this is the report identifier after generate report was clicked
   * @param request
   * @throws Exception Thrown when the configured sender class is not found or fails to initialize or the reportId it not found
   */
  @PostMapping("/{reportId}/$send")
  public Bundle send(
          @AuthenticationPrincipal LinkCredentials user,
          @PathVariable String reportId,
          @PathVariable String tenantId,
          @RequestParam(required = false, defaultValue = "false") boolean download,
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

    //noinspection unused
    try (Stopwatch stopwatch = this.stopwatchManager.start(Constants.TASK_SUBMIT, Constants.CATEGORY_SUBMISSION)) {
      sender.send(this.eventService, tenantService, report, request, user);
    }
    FhirHelper.incrementMajorVersion(report);
    report.setStatus(ReportStatuses.Submitted);
    report.setSubmittedTime(new Date());


    stopwatchManager.storeMetrics(tenantId, reportId, report.getVersion());

    tenantService.saveReport(report);

    this.sharedService.audit(user, request, tenantService, AuditTypes.Submit, String.format("Submitted report %s", reportId));

    if (download) {
      FhirBundler bundler = new FhirBundler(this.eventService, this.sharedService, tenantService);
      return bundler.generateBundle(report);
    } else {
      return null;
    }
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

    List<Aggregate> aggregates = tenantService.getAggregates(report.getId());
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

    return tenantService.getPatientLists(report.getId());
  }

  @GetMapping("/{reportId}/individual")
  public List<MeasureReport> getPatientMeasureReports(@PathVariable String tenantId, @PathVariable String reportId) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);

    if (tenantService == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    Report report = tenantService.getReport(reportId);

    if (report == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Report %s not found", reportId));
    }

    return tenantService.getPatientMeasureReports(reportId).stream()
            .map(PatientMeasureReport::getMeasureReport)
            .collect(Collectors.toList());
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

  @GetMapping(path = "/{reportId}/{version}/$insights", produces = "text/html")
  public String getInsights(@PathVariable String tenantId, @PathVariable String reportId, @PathVariable String version) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);

    if (tenantService == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    String styles;
    try {
      styles = IOUtils.resourceToString("/report.css", StandardCharsets.UTF_8);
    } catch (IOException e) {
      styles = "";
    }
    String content = this.sharedService.getReportInsights(tenantId, reportId, version)
            + tenantService.getReportInsights(tenantId, reportId, version);
    StringBuilder result = new StringBuilder();
    result.append("<html><head>");
    result.append(String.format("<style type=\"text/css\">%s</style>", styles));
    result.append("</head><body>");
    result.append(String.format("<div class=\"container\">%s</div>", content));
    result.append("</body></html>");
    return result.toString();
  }

  @GetMapping
  public List<Report> searchReports(@PathVariable String tenantId) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);

    if (tenantService == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    return tenantService.searchReports();
  }
}
