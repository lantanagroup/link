package com.lantanagroup.link;

import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.PatientData;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import com.lantanagroup.link.time.Stopwatch;
import com.lantanagroup.link.time.StopwatchManager;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.r4.R4MeasureService;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MeasureEvaluator {
  private static final Logger logger = LoggerFactory.getLogger(MeasureEvaluator.class);
  private final ReportCriteria criteria;
  private final ReportContext reportContext;
  private final ReportContext.MeasureContext measureContext;
  private final ApiConfig config;
  private final String patientId;
  private final StopwatchManager stopwatchManager;
  private final TenantService tenantService;

  private MeasureEvaluator(TenantService tenantService, StopwatchManager stopwatchManager, ReportCriteria criteria, ReportContext reportContext, ReportContext.MeasureContext measureContext, ApiConfig config, String patientId) {
    this.tenantService = tenantService;
    this.stopwatchManager = stopwatchManager;
    this.criteria = criteria;
    this.reportContext = reportContext;
    this.measureContext = measureContext;
    this.config = config;
    this.patientId = patientId;
  }

  public static MeasureReport generateMeasureReport(TenantService tenantService, StopwatchManager stopwatchManager, ReportCriteria criteria, ReportContext reportContext, ReportContext.MeasureContext measureContext, ApiConfig config, PatientOfInterestModel patientOfInterest) {
    MeasureEvaluator evaluator = new MeasureEvaluator(tenantService, stopwatchManager, criteria, reportContext, measureContext, config, patientOfInterest.getId());
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

  private static R4MeasureService getR4MeasureService(InMemoryFhirRepository repo) {

    // TODO: setup the measure evaluation options to share library / terminology dependencies
    return new R4MeasureService(repo, MeasureEvaluationOptions.defaultOptions());
  }

  private MeasureReport generateMeasureReport() {
    String patientDataBundleId = ReportIdHelper.getPatientDataBundleId(reportContext.getMasterIdentifierValue(), patientId);
    String measureId = this.measureContext.getMeasure().getIdElement().getIdPart();
    String start = this.criteria.getPeriodStart().substring(0, this.criteria.getPeriodStart().indexOf("."));
    String end = this.criteria.getPeriodEnd().substring(0, this.criteria.getPeriodEnd().indexOf("."));

    InMemoryFhirRepository repo = new InMemoryFhirRepository(FhirContextProvider.getFhirContext());
    try (Stopwatch stopwatch = this.stopwatchManager.start("retrieve-patient-data")) {
      List<PatientData> patientDataList = tenantService.findPatientData(patientId);
      //Bundle additionalDataBundle = PatientData.asBundle(patientDataList);
      //String json = FhirContextProvider.getFhirContext().newJsonParser().encodeResourceToString(additionalData)
      for (PatientData patientData : patientDataList) {
        repo.update(patientData.getResource());
      }
    }

    R4MeasureService measureService = getR4MeasureService(repo);
    Endpoint terminologyEndpoint = null;

    if (StringUtils.isNotEmpty(this.config.getTerminologyService())) {
      logger.info("evaluate-measure is being executed with the terminologyEndpoint parameter.");
      terminologyEndpoint = getTerminologyEndpoint(this.config);
    }

    logger.info(String.format("Evaluating measure for patient %s and measure %s", patientId, measureId));
    MeasureReport measureReport = measureService.evaluate(
            Eithers.forRight3(this.measureContext.getMeasure()),
            criteria.getPeriodStart(),
            criteria.getPeriodEnd(),
            null,
            patientId,
            null, null,
            terminologyEndpoint, null, null);

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

      // group.measureScore is required for DEQM profile validation
      measureReport.getGroup().stream()
              .filter(g -> g.getMeasureScore() == null || g.getMeasureScore().getValue() == null)
              .forEach(g -> {
                g.getMeasureScore().addExtension(Constants.DataAbsentReasonExtensionUrl, new CodeType(Constants.DataAbsentReasonUnknownCode));
              });

      logger.info(String.format("Done generating measure report for %s", patientDataBundleId));
    }

    return measureReport;
  }
}
