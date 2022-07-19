package com.lantanagroup.link.api;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.model.QueryResponse;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeasureEvaluator {
  private static final Logger logger = LoggerFactory.getLogger(MeasureEvaluator.class);
  private ReportCriteria criteria;
  private ReportContext context;
  private ApiConfig config;
  private String patientId;


  private MeasureEvaluator(ReportCriteria criteria, ReportContext context, ApiConfig config, String patientId) {
    this.criteria = criteria;
    this.context = context;
    this.config = config;
    this.patientId = patientId;
  }

  public static MeasureReport generateMeasureReport(ReportCriteria criteria, ReportContext context, ApiConfig config, String patientId) {
    MeasureEvaluator evaluator = new MeasureEvaluator(criteria, context, config, patientId);
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

    try {
      logger.info(String.format("Executing $evaluate-measure for %s", this.context.getMeasureId()));


      QueryResponse patientData = context.getPatientData().stream().filter(e -> e.getPatientId() == patientId).findFirst().get();

      Parameters parameters = new Parameters();
      parameters.addParameter().setName("periodStart").setValue(new StringType(this.criteria.getPeriodStart().substring(0, this.criteria.getPeriodStart().indexOf("."))));
      parameters.addParameter().setName("periodEnd").setValue(new StringType(this.criteria.getPeriodEnd().substring(0, this.criteria.getPeriodEnd().indexOf("."))));
      parameters.addParameter().setName("subject").setValue(new StringType(patientId));
      parameters.addParameter().setName("additionalData").setResource(patientData.getBundle());
      if(!this.config.getEvaluationService().equals(this.config.getTerminologyService())) {
        Endpoint terminologyEndpoint = getTerminologyEndpoint(this.config);
        parameters.addParameter().setName("terminologyEndpoint").setResource(terminologyEndpoint);
        logger.info("evaluate-measure is being executed with the terminologyEndpoint parameter.");
      }

      FhirDataProvider fhirDataProvider = new FhirDataProvider(this.config.getEvaluationService());
      measureReport = fhirDataProvider.getMeasureReport(this.context.getMeasureId(), parameters);

      logger.info(String.format("Done executing $evaluate-measure for %s", this.context.getMeasureId()));

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

        logger.info(String.format("Done generating measure report for %s-%s", this.context.getReportId(), this.patientId.hashCode()));
        measureReport.setId(this.context.getReportId());
        this.context.setMeasureReport(measureReport);
      }
    } catch (Exception e) {
      logger.error(String.format("Error evaluating Measure Report for patient bundle %s-%s: %s", this.context.getReportId(), this.patientId.hashCode(), e.getMessage()));
      throw e;
    }

    return measureReport;
  }
}
