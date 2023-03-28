package com.lantanagroup.link.api;

import com.lantanagroup.link.IReportAggregator;
import com.lantanagroup.link.ReportIdHelper;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.db.MongoService;
import com.lantanagroup.link.db.model.PatientMeasureReport;
import com.lantanagroup.link.db.model.Report;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
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
import java.util.stream.Collectors;

/**
 * This class creates a master measure report based on every individual report generated for each patient included in the "census" list..
 */
public class ReportGenerator {
  private static final Logger logger = LoggerFactory.getLogger(ReportGenerator.class);

  private ReportContext reportContext;
  private ReportContext.MeasureContext measureContext;
  private ReportCriteria criteria;
  private LinkCredentials user;
  private ApiConfig config;
  private IReportAggregator reportAggregator;
  private StopwatchManager stopwatchManager;
  private MongoService mongoService;
  private Report report;

  public ReportGenerator(MongoService mongoService, StopwatchManager stopwatchManager, ReportContext reportContext, ReportContext.MeasureContext measureContext, ReportCriteria criteria, ApiConfig config, LinkCredentials user, IReportAggregator reportAggregator, Report report) {
    this.mongoService = mongoService;
    this.stopwatchManager = stopwatchManager;
    this.reportContext = reportContext;
    this.measureContext = measureContext;
    this.criteria = criteria;
    this.user = user;
    this.config = config;
    this.reportAggregator = reportAggregator;
    this.report = report;
  }

  /**
   * This method accepts a list of patients and generates an individual measure report for each patient. Then agregates all the individual reports into a master measure report.
   */
  public void generate() throws ParseException, ExecutionException, InterruptedException {
    if (this.config.getEvaluationService() == null) {
      throw new IllegalStateException("api.evaluation-service has not been configured");
    }
    logger.info("Patient list is : " + measureContext.getPatientsOfInterest().size());
    ForkJoinPool forkJoinPool = config.getMeasureEvaluationThreads() != null
            ? new ForkJoinPool(config.getMeasureEvaluationThreads())
            : ForkJoinPool.commonPool();
    try {
      List<MeasureReport> patientMeasureReports = forkJoinPool.submit(() ->
              measureContext.getPatientsOfInterest().parallelStream().filter(patient -> {
                return StringUtils.isNotEmpty(patient.getReference()) || StringUtils.isNotEmpty(patient.getIdentifier());
              }).map(patient -> {
                String measureReportId = ReportIdHelper.getPatientMeasureReportId(measureContext.getReportId(), patient.getId());
                PatientMeasureReport patientMeasureReport = new PatientMeasureReport();
                patientMeasureReport.setId(measureReportId);
                patientMeasureReport.setPeriodStart(criteria.getPeriodStart());
                patientMeasureReport.setPeriodEnd(criteria.getPeriodEnd());
                patientMeasureReport.setMeasureId(measureContext.getBundleId());
                patientMeasureReport.setPatientId(patient.getId());

                logger.info("Generating measure report for patient " + patient);
                MeasureReport measureReport = MeasureEvaluator.generateMeasureReport(this.mongoService, this.stopwatchManager, criteria, reportContext, measureContext, config, patient);
                measureReport.setId(measureReportId);
                patientMeasureReport.setMeasureReport(measureReport);

                logger.info(String.format("Persisting patient %s measure report with id %s", patient, measureReportId));
                try (Stopwatch stopwatch = this.stopwatchManager.start("store-measure-report")) {
                  this.mongoService.savePatientMeasureReport(patientMeasureReport);
                }

                return measureReport;
              }).collect(Collectors.toList())).get();
      // to avoid thread collision remove saving the patientMeasureReport on the FhirServer from the above parallelStream
      // pass them to aggregators using measureContext
      this.measureContext.setPatientReports(patientMeasureReports);
    } finally {
      if (forkJoinPool != null) {
        forkJoinPool.shutdown();
      }
    }

    MeasureReport masterMeasureReport = this.reportAggregator.generate(this.criteria, this.reportContext, this.measureContext);
    this.measureContext.setMeasureReport(masterMeasureReport);
    this.report.getAggregates().add(masterMeasureReport);
  }

  public MeasureReport getMeasureReport() {
    return this.measureContext.getMeasureReport();
  }
}
