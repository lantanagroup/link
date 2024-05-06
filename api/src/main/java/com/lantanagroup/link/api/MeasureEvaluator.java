package com.lantanagroup.link.api;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.Helper;
import com.lantanagroup.link.ReportIdHelper;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.db.TenantService;
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
  private TenantService tenantService;
  private MeasureServiceWrapper measureServiceWrapper;

  private MeasureEvaluator(TenantService tenantService, MeasureServiceWrapper measureServiceWrapper, StopwatchManager stopwatchManager, ReportCriteria criteria, ReportContext reportContext, ReportContext.MeasureContext measureContext, ApiConfig config, String patientId) {
    this.tenantService = tenantService;
    this.measureServiceWrapper = measureServiceWrapper;
    this.stopwatchManager = stopwatchManager;
    this.criteria = criteria;
    this.reportContext = reportContext;
    this.measureContext = measureContext;
    this.config = config;
    this.patientId = patientId;
  }

  public static MeasureReport generateMeasureReport(TenantService tenantService, MeasureServiceWrapper measureServiceWrapper, StopwatchManager stopwatchManager, ReportCriteria criteria, ReportContext reportContext, ReportContext.MeasureContext measureContext, ApiConfig config, PatientOfInterestModel patientOfInterest) {
    MeasureEvaluator evaluator = new MeasureEvaluator(tenantService, measureServiceWrapper, stopwatchManager, criteria, reportContext, measureContext, config, patientOfInterest.getId());
    return evaluator.generateMeasureReport();
  }

  private MeasureReport generateMeasureReport() {
    MeasureReport measureReport;
    String patientDataBundleId = ReportIdHelper.getPatientDataBundleId(reportContext.getMasterIdentifierValue(), patientId);
    String measureId = this.measureContext.getMeasure().getIdElement().getIdPart();

    Bundle patientBundle;
    try (Stopwatch stopwatch = this.stopwatchManager.start(Constants.TASK_RETRIEVE_PATIENT_DATA, Constants.CATEGORY_REPORT)) {
      patientBundle = PatientData.asBundle(tenantService.findPatientData(reportContext.getMasterIdentifierValue(), patientId));
    }

    // if patient is in the debugPatients list or debugPatients = "*" then write the patient data bundle to the file system
    if (reportContext.getDebugPatients().contains("Patient/" + patientId) || reportContext.getDebugPatients().contains("*")) {
      String fileName = ReportIdHelper.getPatientBundleFileName(reportContext.getMasterIdentifierValue(), patientId) + ".json";
      Helper.dumpToFile(patientBundle, config.getDebugPath(), fileName);
    }

    logger.info("Executing $evaluate-measure for measure: {}, start: {}, end: {}, patient: {}, resources: {}", measureId, criteria.getPeriodStart(), criteria.getPeriodEnd(), patientId, patientBundle.getEntry().size());

    //noinspection unused
    try (Stopwatch stopwatch = this.stopwatchManager.start(Constants.TASK_MEASURE, Constants.CATEGORY_EVALUATE)) {
      measureReport = measureServiceWrapper.evaluate(this.criteria.getPeriodStart(), this.criteria.getPeriodEnd(), patientId, patientBundle);
    }

    // TODO: commenting out this code because the narrative text isn't being generated, will need to look into this
    // fhirContext.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
    // String output = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(measureReport);

    if (null != measureReport) {
      // Explicitly set the measure URL, appending the version if available
      Measure measure = this.measureContext.getMeasure();
      String measureUrl = measure.getUrl();
      if (measure.hasVersion()) {
        measureUrl += "|" + measure.getVersion();
      }
      measureReport.setMeasure(measureUrl);

      // Fix the measure report's evaluatedResources to make sure resource references are correctly formatted
      for (Reference evaluatedResource : measureReport.getEvaluatedResource()) {
        if (!evaluatedResource.hasReference()) continue;

        if (evaluatedResource.getReference().matches("^#[A-Z].+/.+$")) {
          String newReference = evaluatedResource.getReference().substring(1);
          evaluatedResource.setReference(newReference);
        }
      }

      // This extension is required for profile validation against DEQM
      if (!measureReport.hasExtension(Constants.MeasureScoringExtension)) {
        measureReport.addExtension(Constants.MeasureScoringExtension, this.measureContext.getMeasure().getScoring());
      }

      // improvementNotation is required for DEQM profile validation
      if (measureReport.getImprovementNotation() == null || !measureReport.getImprovementNotation().hasCoding()) {
        String improvementNotationCode;
        if (measureReport.getGroup().size() > 0 && measureReport.getGroup().get(0).getPopulation().size() > 0 && measureReport.getGroup().get(0).getPopulation().get(0).getCount() > 0) {
          improvementNotationCode = "increase";
        } else {
          improvementNotationCode = "decrease";
        }

        CodeableConcept improvementNotation = new CodeableConcept();
        improvementNotation.addCoding()
                .setSystem(Constants.MeasureImprovementNotationCodeSystem)
                .setCode(improvementNotationCode);
        measureReport.setImprovementNotation(improvementNotation);
      }

      // group.measureScore is required for DEQM profile validation of non-cohort measures
      if(measure.getScoring().hasCoding() &&
              measure.getScoring().getCoding().stream().noneMatch(c -> c.hasCode() && c.getCode().equals("cohort"))) {
        measureReport.getGroup().stream()
                .filter(g -> g.getMeasureScore() == null || g.getMeasureScore().getValue() == null)
                .forEach(g -> {
                  g.getMeasureScore().addExtension(Constants.DataAbsentReasonExtensionUrl, new CodeType(Constants.DataAbsentReasonUnknownCode));
                });
      }
      logger.info(String.format("Done generating measure report for %s", patientDataBundleId));
    }

    return measureReport;
  }
}
