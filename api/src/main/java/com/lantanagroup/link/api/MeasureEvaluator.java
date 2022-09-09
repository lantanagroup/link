package com.lantanagroup.link.api;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.instance.model.api.IBaseResource;
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

  public static MeasureReport generateMeasureReport(ReportCriteria criteria, ReportContext context, ApiConfig config, PatientOfInterestModel patientOfInterest) {
    MeasureEvaluator evaluator = new MeasureEvaluator(criteria, context, config, patientOfInterest.getId());
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

      // get patient bundle from the fhirserver
      FhirDataProvider fhirStoreProvider = new FhirDataProvider(this.config.getDataStore());
      IBaseResource patientBundle = fhirStoreProvider.getBundleById(context.getReportId() + "-" + patientId.hashCode());
      Parameters parameters = new Parameters();
      parameters.addParameter().setName("periodStart").setValue(new StringType(this.criteria.getPeriodStart().substring(0, this.criteria.getPeriodStart().indexOf("."))));
      parameters.addParameter().setName("periodEnd").setValue(new StringType(this.criteria.getPeriodEnd().substring(0, this.criteria.getPeriodEnd().indexOf("."))));
      parameters.addParameter().setName("subject").setValue(new StringType(patientId));
      parameters.addParameter().setName("additionalData").setResource((Bundle) patientBundle);
      if (!this.config.getEvaluationService().equals(this.config.getTerminologyService())) {
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
        // TODO: Remove this; ReportGenerator.generate already does it (correctly, unlike here)
        measureReport.setId(this.context.getReportId());
        // TODO: Remove this; it's expected to be the summary report, not an individual report
        //       Though maybe it would be helpful to collect the individual reports in the context as well
        //       That way, we wouldn't have to retrieve them from the data store service during aggregation
        this.context.setMeasureReport(measureReport);
      }
    } catch (Exception e) {
      logger.error(String.format("Error evaluating Measure Report for patient bundle %s-%s", this.context.getReportId(), this.patientId.hashCode()));
      throw e;
    }

    return measureReport;
  }
}
