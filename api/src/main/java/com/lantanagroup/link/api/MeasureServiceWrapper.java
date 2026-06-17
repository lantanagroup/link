package com.lantanagroup.link.api;

import ca.uhn.fhir.repository.IRepository;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirContextProvider;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReference;
import org.opencds.cqf.fhir.cr.measure.r4.R4MultiMeasureService;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class MeasureServiceWrapper {
  private static final Logger logger = LoggerFactory.getLogger(MeasureServiceWrapper.class);
  private final MeasureDef measureDef;
  private final Endpoint terminologyEndpoint;
  private final MeasureEvaluationOptions options;

  public MeasureServiceWrapper(Bundle measureDefBundle, String terminologyService) {
    measureDef = new MeasureDef(measureDefBundle);
    terminologyEndpoint = getTerminologyEndpoint(terminologyService);
    options = MeasureEvaluationOptions.defaultOptions();
    EvaluationSettings evaluationSettings = options.getEvaluationSettings();
    evaluationSettings.getTerminologySettings()
            .setValuesetPreExpansionMode(TerminologySettings.VALUESET_PRE_EXPANSION_MODE.USE_IF_PRESENT)
            .setValuesetExpansionMode(TerminologySettings.VALUESET_EXPANSION_MODE.PERFORM_NAIVE_EXPANSION)
            .setValuesetMembershipMode(TerminologySettings.VALUESET_MEMBERSHIP_MODE.USE_EXPANSION)
            .setCodeLookupMode(TerminologySettings.CODE_LOOKUP_MODE.USE_CODESYSTEM_URL);
    evaluationSettings.getRetrieveSettings()
            .setTerminologyParameterMode(RetrieveSettings.TERMINOLOGY_FILTER_MODE.FILTER_IN_MEMORY)
            .setSearchParameterMode(RetrieveSettings.SEARCH_FILTER_MODE.FILTER_IN_MEMORY)
            .setProfileMode(RetrieveSettings.PROFILE_MODE.DECLARED);
  }

  private static Endpoint getTerminologyEndpoint(String terminologyService) {
    if (StringUtils.isEmpty(terminologyService)) {
      return null;
    }
    Endpoint endpoint = new Endpoint();
    endpoint.setStatus(Endpoint.EndpointStatus.ACTIVE);
    endpoint.setConnectionType(new Coding());
    endpoint.getConnectionType().setSystem(Constants.TerminologyEndpointSystem);
    endpoint.getConnectionType().setCode(Constants.TerminologyEndpointCode);
    endpoint.setAddress(terminologyService);
    return endpoint;
  }

  public void preCompile() {
    try {
      String subject = "Patient/the-patient";
      Patient patient = new Patient();
      patient.setId(subject);
      Bundle additionalData = new Bundle();
      additionalData.addEntry().setResource(patient);
      evaluate("2024-01-01", "2024-01-31", subject, additionalData);
    } catch (Exception e) {
      logger.warn("CQL pre-compilation failed (non-fatal); first evaluation will compile on demand: {}", e.getMessage());
    }
  }

  public MeasureReport evaluate(String periodStart, String periodEnd, String subject, Bundle additionalData) {
    IRepository repository = new InMemoryFhirRepository(FhirContextProvider.getFhirContext());
    for (IBaseResource resource : measureDef.getResources()) {
      repository.update(resource);
    }
    if (additionalData != null) {
      for (Bundle.BundleEntryComponent entry : additionalData.getEntry()) {
        if (entry.getResource() != null) {
          repository.update(entry.getResource());
        }
      }
    }
    R4MultiMeasureService measureService = new R4MultiMeasureService(repository, options, null, new MeasurePeriodValidator());
    return measureService.evaluate(
            new MeasureReference.ByCanonicalUrl(measureDef.getMeasure().getUrl()),
            parsePeriodStart(periodStart),
            parsePeriodEnd(periodEnd),
            null,
            subject,
            null,
            null,
            null,
            null);
  }

  private static ZonedDateTime parsePeriodStart(String dateStr) {
    if (dateStr == null) return null;
    try {
      return ZonedDateTime.parse(dateStr);
    } catch (Exception e) {
      return LocalDate.parse(dateStr).atStartOfDay(ZoneOffset.UTC);
    }
  }

  private static ZonedDateTime parsePeriodEnd(String dateStr) {
    if (dateStr == null) return null;
    try {
      return ZonedDateTime.parse(dateStr);
    } catch (Exception e) {
      return LocalDate.parse(dateStr).atTime(LocalTime.MAX).atZone(ZoneOffset.UTC);
    }
  }
}
