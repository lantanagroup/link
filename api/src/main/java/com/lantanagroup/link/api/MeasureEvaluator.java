package com.lantanagroup.link.api;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.config.api.ApiConfig;
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

  private MeasureEvaluator(ReportCriteria criteria, ReportContext context, ApiConfig config) {
    this.criteria = criteria;
    this.context = context;
    this.config = config;
  }

  public static MeasureReport generateMeasureReport(ReportCriteria criteria, ReportContext context, ApiConfig config) {
    MeasureEvaluator evaluator = new MeasureEvaluator(criteria, context, config);
    return evaluator.generateMeasureReport();
  }

  private MeasureReport generateMeasureReport() {
    MeasureReport measureReport = null;

    String url = this.config.getFhirServerStore();
    if (!url.endsWith("/")) url += "/";
    url += "Measure/" + this.context.getMeasureId() + "/$evaluate-measure?" +
            "periodStart=" + this.criteria.getPeriodStart() + "&" +
            "periodEnd=" + this.criteria.getPeriodEnd();

    try {
      logger.info(String.format("Executing $evaluate-measure for %s", this.context.getMeasureId()));
      measureReport = this.context.getFhirStoreClient().fetchResourceFromUrl(MeasureReport.class, url);
      logger.info(String.format("Done executing $evaluate-measure for %s", this.context.getMeasureId()));

      if (this.config.getMeasureLocation() != null) {
        logger.debug("Creating MeasureReport.subject based on config");
        Reference subjectRef = new Reference();

        if (this.config.getMeasureLocation().getSystem() != null || this.config.getMeasureLocation().getValue() != null) {
          subjectRef.setIdentifier(new Identifier()
                  .setSystem(this.config.getMeasureLocation().getSystem())
                  .setValue(this.config.getMeasureLocation().getValue()));
        }

        if (this.config.getMeasureLocation().getLatitude() != null || this.config.getMeasureLocation().getLongitude() != null) {
          Extension positionExt = new Extension(Constants.ReportPositionExtUrl);

          if (this.config.getMeasureLocation().getLongitude() != null) {
            Extension longExt = new Extension("longitude");
            longExt.setValue(new DecimalType(this.config.getMeasureLocation().getLongitude()));
            positionExt.addExtension(longExt);
          }

          if (this.config.getMeasureLocation().getLatitude() != null) {
            Extension latExt = new Extension("latitude");
            latExt.setValue(new DecimalType(this.config.getMeasureLocation().getLatitude()));
            positionExt.addExtension(latExt);
          }

          subjectRef.addExtension(positionExt);
        }

        measureReport.setSubject(subjectRef);
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

        logger.info("Done generating measure report, setting response answer to JSON of MeasureReport");
        measureReport.setId(this.context.getReportId());
        this.context.setMeasureReport(measureReport);
      }
    } catch (Exception e) {
      logger.error("Error generating Measure Report: " + e.getMessage());
      throw e;
    }

    return measureReport;
  }
}
