package com.lantanagroup.link.api;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.IReportAggregator;
import com.lantanagroup.link.ReportIdHelper;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.Aggregate;
import com.lantanagroup.link.db.model.PatientMeasureReport;
import com.lantanagroup.link.db.model.Report;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import com.lantanagroup.link.query.QueryPhase;
import com.lantanagroup.link.time.Stopwatch;
import com.lantanagroup.link.time.StopwatchManager;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.MeasureReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class creates a master measure report based on every individual report generated for each patient included in the "census" list..
 */
public class ReportGenerator {
  private static final Logger logger = LoggerFactory.getLogger(ReportGenerator.class);

  private final ReportContext reportContext;
  private final ReportContext.MeasureContext measureContext;
  private final ReportCriteria criteria;
  private final ApiConfig config;
  private final IReportAggregator reportAggregator;
  private final StopwatchManager stopwatchManager;
  private final SharedService sharedService;
  private final TenantService tenantService;
  private final Report report;

  public ReportGenerator(SharedService sharedService, TenantService tenantService, StopwatchManager stopwatchManager, ReportContext reportContext, ReportContext.MeasureContext measureContext, ReportCriteria criteria, ApiConfig config, IReportAggregator reportAggregator, Report report) {
    this.sharedService = sharedService;
    this.tenantService = tenantService;
    this.stopwatchManager = stopwatchManager;
    this.reportContext = reportContext;
    this.measureContext = measureContext;
    this.criteria = criteria;
    this.config = config;
    this.reportAggregator = reportAggregator;
    this.report = report;
  }

  /**
   * This method accepts a list of patients and generates an individual measure report for each patient.
   */
  public void generate(QueryPhase queryPhase) throws ExecutionException, InterruptedException {
    MeasureService measureService = new MeasureService(measureContext.getReportDefBundle(), config.getTerminologyService());
    measureService.preCompile();
    logger.info("Patient list is : " + measureContext.getPatientsOfInterest(queryPhase).size());
    ForkJoinPool forkJoinPool = config.getMeasureEvaluationThreads() != null
            ? new ForkJoinPool(config.getMeasureEvaluationThreads())
            : ForkJoinPool.commonPool();
    AtomicInteger progress = new AtomicInteger(0);
    List<PatientOfInterestModel> pois = measureContext.getPatientsOfInterest(queryPhase);

    try {
      forkJoinPool.submit(() -> pois.parallelStream().forEach(patient -> {
                if (StringUtils.isEmpty(patient.getId())) {
                  logger.error("Patient {} has no ID; cannot generate measure report", patient);
                  return;
                }
                try {
                  MeasureReport measureReport = generate(measureService, patient);
                  synchronized (this) {
                    measureContext.getPatientReportsByPatientId().put(patient.getId(), measureReport);
                  }
                } catch (Exception e) {
                  logger.error("Error generating measure report for patient {}", patient.getId(), e);
                } finally {
                  int completed = progress.incrementAndGet();
                  double percent = Math.round((completed * 100.0) / pois.size());
                  logger.info("Progress ({}%) for report {} is {} of {}", String.format("%.2f", percent), reportContext.getMasterIdentifierValue(), completed, pois.size());
                }
              }))
              .get();
    } finally {
      if (forkJoinPool != null) {
        forkJoinPool.shutdown();
      }
    }
  }

  private MeasureReport generate(MeasureService measureService, PatientOfInterestModel patient) {
    String measureReportId = ReportIdHelper.getPatientMeasureReportId(measureContext.getReportId(), patient.getId());
    PatientMeasureReport patientMeasureReport = new PatientMeasureReport();
    patientMeasureReport.setId(measureReportId);
    patientMeasureReport.setReportId(reportContext.getMasterIdentifierValue());
    patientMeasureReport.setMeasureId(measureContext.getBundleId());
    patientMeasureReport.setPatientId(patient.getId());

    logger.info("Generating measure report for patient " + patient);

    MeasureReport measureReport = MeasureEvaluator.generateMeasureReport(this.tenantService, measureService, this.stopwatchManager, criteria, reportContext, measureContext, config, patient);
    measureReport.setId(measureReportId);
    patientMeasureReport.setMeasureReport(measureReport);

    logger.info(String.format("Persisting patient %s measure report with id %s", patient, measureReportId));
    //noinspection unused
    try (Stopwatch stopwatch = this.stopwatchManager.start(Constants.TASK_STORE_MEASURE_REPORT, Constants.CATEGORY_REPORT)) {
      this.tenantService.savePatientMeasureReport(patientMeasureReport);
    }

    return measureReport;
  }

  public void aggregate() throws ParseException {
    MeasureReport masterMeasureReport = this.reportAggregator.generate(this.criteria, this.measureContext);
    this.measureContext.setMeasureReport(masterMeasureReport);

    Aggregate aggregateReport = new Aggregate();
    aggregateReport.setId(this.measureContext.getReportId());
    aggregateReport.setReportId(this.reportContext.getMasterIdentifierValue());
    aggregateReport.setMeasureId(this.measureContext.getBundleId());
    aggregateReport.setReport(masterMeasureReport);
    this.tenantService.saveAggregate(aggregateReport);
  }
}
