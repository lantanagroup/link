package com.lantanagroup.link.api;

import com.lantanagroup.link.*;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
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

  public ReportGenerator(StopwatchManager stopwatchManager, ReportContext reportContext, ReportContext.MeasureContext measureContext, ReportCriteria criteria, ApiConfig config, LinkCredentials user, IReportAggregator reportAggregator) {
    this.stopwatchManager = stopwatchManager;
    this.reportContext = reportContext;
    this.measureContext = measureContext;
    this.criteria = criteria;
    this.user = user;
    this.config = config;
    this.reportAggregator = reportAggregator;
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
              measureContext.getPatientsOfInterest().parallelStream().filter(patient -> !StringUtils.isEmpty(patient.getId())).map(patient -> {

                logger.info("Generating measure report for patient " + patient);
                MeasureReport patientMeasureReport = new MeasureReport();
                try {
                  patientMeasureReport = MeasureEvaluator.generateMeasureReport(this.stopwatchManager, criteria, reportContext, measureContext, config, patient);
                } catch (Exception ex) {
                  logger.error(String.format("Issue generating patient measure report for %s, error %s", patient, ex.getMessage()));
                }

                String measureReportId = ReportIdHelper.getPatientMeasureReportId(measureContext.getReportId(), patient.getId());
                patientMeasureReport.setId(measureReportId);
                // Tag individual MeasureReport as patient-data as it references a patient and will be found for expunge
                patientMeasureReport.getMeta().addTag(Constants.MainSystem, Constants.patientDataTag,"");

                logger.info(String.format("Persisting patient %s measure report with id %s", patient, measureReportId));
                Stopwatch stopwatch = this.stopwatchManager.start("store-measure-report");
                this.reportContext.getFhirProvider().updateResource(patientMeasureReport);
                stopwatch.stop();

                return patientMeasureReport;
              }).collect(Collectors.toList())).get();
      // to avoid thread collision remove saving the patientMeasureReport on the FhirServer from the above parallelStream
      // pass them to aggregators using measureContext
      measureContext.setPatientReports(patientMeasureReports);
    } finally {
      if (forkJoinPool != null) {
        forkJoinPool.shutdown();
      }
    }
    MeasureReport masterMeasureReport = reportAggregator.generate(criteria, reportContext, measureContext);
    measureContext.setMeasureReport(masterMeasureReport);
  }

  /**
   * Stores the individual patient reports on the Fhir Server in batches of 50 for performance reasons
   * Stores the master measure report on the Fhir Server.
   **/
  public void store() {

    this.measureContext.getPatientReports().parallelStream().forEach(report -> {
      this.reportContext.getFhirProvider().updateResource(report);
    });

    this.reportContext.getFhirProvider().updateResource(measureContext.getMeasureReport());
  }
}
