package com.lantanagroup.link.api;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.Helper;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

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

  public static void aggregateMeasureReports(MeasureReport master, List<MeasureReport> patientMeasureReports) {
    for (MeasureReport patientMeasureReport : patientMeasureReports) {
      for (MeasureReport.MeasureReportGroupComponent group : patientMeasureReport.getGroup()) {
        for (MeasureReport.MeasureReportGroupPopulationComponent population : group.getPopulation()) {
          // TODO: Check if group and population code exist in master, if not create

          // TODO: Add population.count to the master group/population count

          // TODO: Identify or create the List for this master group/population

          // TODO: If this population incremented the master, add this patient measure report to the contained List
        }
      }
    }
  }

  private MeasureReport generateMeasureReport() {
    MeasureReport measureReport = null;

    try {
      logger.info(String.format("Executing $evaluate-measure for %s", this.context.getMeasureId()));

      Date startDate = Helper.parseFhirDate(this.criteria.getPeriodStart());
      Date endDate = Helper.parseFhirDate(this.criteria.getPeriodEnd());

      Parameters parameters = new Parameters();
      parameters.addParameter().setName("periodStart").setValue(new InstantType(startDate, TemporalPrecisionEnum.SECOND, TimeZone.getDefault()));
      parameters.addParameter().setName("periodEnd").setValue(new InstantType(endDate, TemporalPrecisionEnum.SECOND, TimeZone.getDefault()));
      // TODO: add patient id as parameter to $evaluate-measure

      measureReport = context.getFhirProvider().getMeasureReport(this.context.getMeasureId(), parameters);

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
    } catch (ParseException ex) {
      logger.error("Parsing error generating Measure Report.");
      throw new RuntimeException(ex);
    } catch (Exception e) {
      logger.error("Error generating Measure Report: " + e.getMessage());
      throw e;
    }

    return measureReport;
  }
}
