package com.lantanagroup.link.api;

import com.lantanagroup.link.IReportAggregator;
import com.lantanagroup.link.ReportIdHelper;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.db.MongoService;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

/**
 * This class creates a master measure report based on every individual report generated for each patient included in the "census" list..
 */
public class ReportGenerator {
  private static final Logger logger = LoggerFactory.getLogger(ReportGenerator.class);

  private ReportContext reportContext;
  private ReportContext.MeasureContext measureContext;
  private ReportCriteria criteria;
  private ApiConfig config;
  private IReportAggregator reportAggregator;
  private StopwatchManager stopwatchManager;
  private MongoService mongoService;
  private Report report;

  public ReportGenerator(MongoService mongoService, StopwatchManager stopwatchManager, ReportContext reportContext, ReportContext.MeasureContext measureContext, ReportCriteria criteria, ApiConfig config, IReportAggregator reportAggregator, Report report) {
    this.mongoService = mongoService;
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
    if (this.config.getEvaluationService() == null) {
      throw new IllegalStateException("api.evaluation-service has not been configured");
    }
    logger.info("Patient list is : " + measureContext.getPatientsOfInterest(queryPhase).size());
    ForkJoinPool forkJoinPool = config.getMeasureEvaluationThreads() != null
            ? new ForkJoinPool(config.getMeasureEvaluationThreads())
            : ForkJoinPool.commonPool();
    try {
      forkJoinPool.submit(() -> measureContext.getPatientsOfInterest(queryPhase).parallelStream().forEach(patient -> {
                if (StringUtils.isEmpty(patient.getReference()) && StringUtils.isEmpty(patient.getIdentifier())) {
                  return;
                }
                try {
                  MeasureReport measureReport = generate(patient);
                  synchronized (this) {
                    measureContext.getPatientReportsByPatientId().put(patient.getId(), measureReport);
                  }
                } catch (Exception e) {
                  logger.error("Error generating measure report for patient {}", patient.getId(), e);
                }
              }))
              .get();
    } finally {
      if (forkJoinPool != null) {
        forkJoinPool.shutdown();
      }
    }
  }

  private MeasureReport generate(PatientOfInterestModel patient) {
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
    //noinspection unused
    try (Stopwatch stopwatch = this.stopwatchManager.start("store-measure-report")) {
      this.mongoService.savePatientMeasureReport(patientMeasureReport);
    }

    return measureReport;
  }

  public void aggregate() throws ParseException {
    MeasureReport masterMeasureReport = this.reportAggregator.generate(this.criteria, this.measureContext);
    this.measureContext.setMeasureReport(masterMeasureReport);

    Aggregate aggregateReport = new Aggregate(masterMeasureReport);
    this.mongoService.saveAggregate(aggregateReport);
    this.report.getAggregates().add(aggregateReport.getId());
  }
}
