package com.lantanagroup.link.api;

import com.lantanagroup.link.IReportAggregator;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
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
  private LinkCredentials user;
  private ApiConfig config;
  private IReportAggregator reportAggregator;

  public ReportGenerator(ReportContext reportContext, ReportContext.MeasureContext measureContext, ReportCriteria criteria, ApiConfig config, LinkCredentials user, IReportAggregator reportAggregator) {
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
    logger.info("Patient list is : " + reportContext.getPatientsOfInterest().size());
    ForkJoinPool forkJoinPool = config.getMeasureEvaluationThreads() != null
            ? new ForkJoinPool(config.getMeasureEvaluationThreads())
            : ForkJoinPool.commonPool();
    try {
      // TODO: Limit report generation to POIs for the specified measure
      //       (We're currently generating a report for all POIs across all measures)
      //       May require storing measure-specific POI lists in each measure context during POI lookup
      forkJoinPool.submit(() -> reportContext.getPatientsOfInterest().parallelStream().forEach(patient -> {
        logger.info("Patient is: " + patient);
        if (!StringUtils.isEmpty(patient.getId())) {
          MeasureReport patientMeasureReport = MeasureEvaluator.generateMeasureReport(criteria, reportContext, measureContext, config, patient);
          patientMeasureReport.setId(measureContext.getReportId() + "-" + patient.getId().hashCode());
          // store the measure report
          this.reportContext.getFhirProvider().updateResource(patientMeasureReport);
        }
      })).get();
    } finally {
      forkJoinPool.shutdown();
    }
    MeasureReport masterMeasureReport = reportAggregator.generate(criteria, reportContext, measureContext);
    measureContext.setMeasureReport(masterMeasureReport);

  }

  /**
   * Stores the master measure report on the Fhir Server.
   **/
  public void store() {
    this.reportContext.getFhirProvider().updateResource(measureContext.getMeasureReport());
  }
}
