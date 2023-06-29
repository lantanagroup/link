package com.lantanagroup.link.api;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.*;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class MeasureEvaluator {
  private static final Logger logger = LoggerFactory.getLogger(MeasureEvaluator.class);
  private ReportCriteria criteria;
  private ReportContext reportContext;
  private ReportContext.MeasureContext measureContext;
  private ApiConfig config;
  private String patientId;
  private StopwatchManager stopwatchManager;

  private MeasureEvaluator(StopwatchManager stopwatchManager, ReportCriteria criteria, ReportContext reportContext, ReportContext.MeasureContext measureContext, ApiConfig config, String patientId) {
    this.stopwatchManager = stopwatchManager;
    this.criteria = criteria;
    this.reportContext = reportContext;
    this.measureContext = measureContext;
    this.config = config;
    this.patientId = patientId;
  }

  public static MeasureReport generateMeasureReport(StopwatchManager stopwatchManager, ReportCriteria criteria, ReportContext reportContext, ReportContext.MeasureContext measureContext, ApiConfig config, PatientOfInterestModel patientOfInterest) {
    MeasureEvaluator evaluator = new MeasureEvaluator(stopwatchManager, criteria, reportContext, measureContext, config, patientOfInterest.getId());
    return evaluator.generateMeasureReport();
  }

  private MeasureReport generateMeasureReport() {
    MeasureReport measureReport;
    String patientDataBundleId = ReportIdHelper.getPatientDataBundleId(reportContext.getMasterIdentifierValue(), patientId);

    try {
      String measureId = this.measureContext.getMeasure().getIdElement().getIdPart();
      logger.info(String.format("Executing $evaluate-measure for %s", measureId));

      // get patient bundle from the fhirserver
      FhirDataProvider fhirStoreProvider = new FhirDataProvider(this.config.getDataStore());
      IBaseResource patientBundle = fhirStoreProvider.getBundleById(patientDataBundleId);

      // TODO - remove this is for debug
      if (patientBundle == null) {
        logger.error(String.format("Patient bundle for ID %s is null", patientDataBundleId));
      }

      logger.info("Removing any non-Patient resource with the same ID as the Patient");
      //Filter all resources that aren't the Patient and don't have the same ID as the Patient but also the Patient resource
      ((Bundle) patientBundle).setEntry(((Bundle) patientBundle).getEntry().stream().filter(entry ->
              (!entry.getResource().getIdElement().getIdPart().equals(patientId)
                      && !entry.getResource().getResourceType().toString().equals("Patient"))
                      || (entry.getResource().getIdElement().getIdPart().equals(patientId)
                      && entry.getResource().getResourceType().toString().equals("Patient"))).collect(Collectors.toList()));

      Parameters parameters = new Parameters();
      parameters.addParameter().setName("periodStart").setValue(new StringType(this.criteria.getPeriodStart().substring(0, this.criteria.getPeriodStart().indexOf("."))));
      parameters.addParameter().setName("periodEnd").setValue(new StringType(this.criteria.getPeriodEnd().substring(0, this.criteria.getPeriodEnd().indexOf("."))));
      parameters.addParameter().setName("subject").setValue(new StringType(patientId));
      parameters.addParameter().setName("additionalData").setResource((Bundle) patientBundle);

      logger.info(String.format("Evaluating measure for patient %s and measure %s", patientId, measureId));
      Date measureEvalStartTime = new Date();

      FhirDataProvider fhirDataProvider = new FhirDataProvider(this.config.getEvaluationService());
      Stopwatch stopwatch = this.stopwatchManager.start("evaluate-measure");
      measureReport = fhirDataProvider.getMeasureReport(measureId, parameters);
      stopwatch.stop();

      logger.info(String.format("Done evaluating measure for patient %s and measure %s, took %s milliseconds", patientId, measureId, (new Date()).getTime() - measureEvalStartTime.getTime()));

      // TODO: commenting out this code because the narrative text isn't being generated, will need to look into this
      // fhirContext.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
      // String output = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(measureReport);

      if (null != measureReport) {
        // Fix the measure report's evaluatedResources to make sure resource references are correctly formatted
        for (Reference evaluatedResource : measureReport.getEvaluatedResource()) {
          if (!evaluatedResource.hasReference()) continue;

          if (evaluatedResource.getReference().matches("^#[A-Z].+/.+$")) {
            String newReference = evaluatedResource.getReference().substring(1);
            evaluatedResource.setReference(newReference);
          }
        }

        logger.info(String.format("Done generating measure report for %s", patientDataBundleId));
        // TODO: Remove this; ReportGenerator.generate already does it (correctly, unlike here)
        measureReport.setId(this.measureContext.getReportId());
        // TODO: Remove this; it's expected to be the summary report, not an individual report
        //       Though maybe it would be helpful to collect the individual reports in the context as well
        //       That way, we wouldn't have to retrieve them from the data store service during aggregation
        this.measureContext.setMeasureReport(measureReport);
      }
    } catch (Exception e) {
      logger.error(String.format("Error evaluating Measure Report for patient bundle %s", patientDataBundleId));
      throw e;
    }

    return measureReport;
  }
}
