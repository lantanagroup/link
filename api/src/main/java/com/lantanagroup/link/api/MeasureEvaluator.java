package com.lantanagroup.link.api;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.ReportIdHelper;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.db.MongoService;
import com.lantanagroup.link.db.model.PatientData;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import com.lantanagroup.link.time.Stopwatch;
import com.lantanagroup.link.time.StopwatchManager;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeasureEvaluator {
  private static final Logger logger = LoggerFactory.getLogger(MeasureEvaluator.class);
  private ReportCriteria criteria;
  private ReportContext reportContext;
  private ReportContext.MeasureContext measureContext;
  private ApiConfig config;
  private String patientId;
  private StopwatchManager stopwatchManager;
  private MongoService mongoService;

  private MeasureEvaluator(MongoService mongoService, StopwatchManager stopwatchManager, ReportCriteria criteria, ReportContext reportContext, ReportContext.MeasureContext measureContext, ApiConfig config, String patientId) {
    this.mongoService = mongoService;
    this.stopwatchManager = stopwatchManager;
    this.criteria = criteria;
    this.reportContext = reportContext;
    this.measureContext = measureContext;
    this.config = config;
    this.patientId = patientId;
  }

  public static MeasureReport generateMeasureReport(MongoService mongoService, StopwatchManager stopwatchManager, ReportCriteria criteria, ReportContext reportContext, ReportContext.MeasureContext measureContext, ApiConfig config, PatientOfInterestModel patientOfInterest) {
    MeasureEvaluator evaluator = new MeasureEvaluator(mongoService, stopwatchManager, criteria, reportContext, measureContext, config, patientOfInterest.getId());
    return evaluator.generateMeasureReport();
  }

  private static Endpoint getTerminologyEndpoint(ApiConfig config) {
    Endpoint terminologyEndpoint = new Endpoint();
    terminologyEndpoint.setStatus(Endpoint.EndpointStatus.ACTIVE);
    terminologyEndpoint.setConnectionType(new Coding());
    terminologyEndpoint.getConnectionType().setSystem(Constants.TerminologyEndpointSystem);
    terminologyEndpoint.getConnectionType().setCode(Constants.TerminologyEndpointCode);
    terminologyEndpoint.setAddress(config.getTerminologyService());
    return terminologyEndpoint;
  }

  private MeasureReport generateMeasureReport() {
    MeasureReport measureReport;
    String patientDataBundleId = ReportIdHelper.getPatientDataBundleId(reportContext.getMasterIdentifierValue(), patientId);

    try {
      String measureId = this.measureContext.getMeasure().getIdElement().getIdPart();
      logger.info(String.format("Executing $evaluate-measure for %s", measureId));

      Bundle patientBundle = PatientData.asBundle(this.mongoService.findPatientData(patientId));

      Parameters parameters = new Parameters();
      parameters.addParameter().setName("periodStart").setValue(new StringType(this.criteria.getPeriodStart().substring(0, this.criteria.getPeriodStart().indexOf("."))));
      parameters.addParameter().setName("periodEnd").setValue(new StringType(this.criteria.getPeriodEnd().substring(0, this.criteria.getPeriodEnd().indexOf("."))));
      parameters.addParameter().setName("subject").setValue(new StringType(patientId));
      parameters.addParameter().setName("additionalData").setResource(patientBundle);
      if (!this.config.getEvaluationService().equals(this.config.getTerminologyService())) {
        Endpoint terminologyEndpoint = getTerminologyEndpoint(this.config);
        parameters.addParameter().setName("terminologyEndpoint").setResource(terminologyEndpoint);
        logger.info("evaluate-measure is being executed with the terminologyEndpoint parameter.");
      }

      logger.info(String.format("Evaluating measure for patient %s and measure %s", patientId, measureId));

      FhirDataProvider fhirDataProvider = new FhirDataProvider(this.config.getEvaluationService());
      //noinspection unused
      try (Stopwatch stopwatch = this.stopwatchManager.start("evaluate-measure")) {
        measureReport = fhirDataProvider.getMeasureReport(measureId, parameters);
      }

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
