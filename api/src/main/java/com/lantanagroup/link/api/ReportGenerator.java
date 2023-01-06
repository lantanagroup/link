package com.lantanagroup.link.api;

import com.lantanagroup.link.IReportAggregator;
import com.lantanagroup.link.ReportIdHelper;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
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
    logger.info("Patient list is : " + measureContext.getPatientsOfInterest().size());
    ForkJoinPool forkJoinPool = config.getMeasureEvaluationThreads() != null
            ? new ForkJoinPool(config.getMeasureEvaluationThreads())
            : ForkJoinPool.commonPool();
    try {
      List<MeasureReport> patientMeasureReports = forkJoinPool.submit(() ->
              measureContext.getPatientsOfInterest().parallelStream().filter(patient -> !StringUtils.isEmpty(patient.getId())).map(patient -> {
                logger.info("Patient is: " + patient);
                MeasureReport patientMeasureReport = MeasureEvaluator.generateMeasureReport(criteria, reportContext, measureContext, config, patient);
                patientMeasureReport.setId(ReportIdHelper.getPatientMeasureReportId(measureContext.getReportId(), patient.getId()));
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

    storeIndividualReports(measureContext.getPatientReports());

    this.reportContext.getFhirProvider().updateResource(measureContext.getMeasureReport());
  }

  /**
   * Stores the individual measure report on the Fhir Server.
   * batch it like 50 at a time to increase performance
   **/
  private void storeIndividualReports(List<MeasureReport> patientMeasureReports) {
    int maxCount = 50;
    Bundle updateBundle = new Bundle();
    updateBundle.setType(Bundle.BundleType.BATCH);

    patientMeasureReports.stream().map(patientMeasureReport -> {
      updateBundle.addEntry()
              .setResource(patientMeasureReport)
              .setRequest(new Bundle.BundleEntryRequestComponent()
                      .setMethod(Bundle.HTTPVerb.PUT)
                      .setUrl("MeasureReport/" + patientMeasureReport.getIdElement().getIdPart()));

      return patientMeasureReport;
    }).collect(Collectors.toList());

    int transactionCount = (int) Math.ceil(updateBundle.getEntry().size() / ((double) maxCount));

    logger.debug("Storing measure reports and updated document reference in internal FHIR Server. " + transactionCount + " bundles total.");

    for (int i = 0; i < transactionCount; i++) {
      Bundle updateBundleCopy = new Bundle();
      updateBundleCopy.setType(Bundle.BundleType.BATCH);

      List<Bundle.BundleEntryComponent> nextEntries = updateBundle.getEntry().subList(0, updateBundle.getEntry().size() >= maxCount ? maxCount : updateBundle.getEntry().size());
      updateBundleCopy.getEntry().addAll(nextEntries);
      updateBundle.getEntry().removeAll(nextEntries);

      logger.debug("Processing bundle " + (i + 1));

      // Execute the transaction of updates on the internal FHIR server for MeasureReports and doc ref
      this.reportContext.getFhirProvider().transaction(updateBundleCopy);

      logger.debug("Done processing bundle " + (i + 1));
    }
  }
}
