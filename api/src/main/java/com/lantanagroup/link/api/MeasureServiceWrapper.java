package com.lantanagroup.link.api;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirContextProvider;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.r4.R4MeasureService;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

public class MeasureServiceWrapper {
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
    String subject = "Patient/the-patient";
    Patient patient = new Patient();
    patient.setId(subject);
    Bundle additionalData = new Bundle();
    additionalData.addEntry().setResource(patient);
    evaluate(null, null, subject, additionalData);
  }

  public MeasureReport evaluate(String periodStart, String periodEnd, String subject, Bundle additionalData) {
    Repository repository = new InMemoryFhirRepository(FhirContextProvider.getFhirContext());
    for (IBaseResource resource : measureDef.getResources()) {
      repository.update(resource);
    }
    R4MeasureService measureService = new R4MeasureService(repository, options);
    return measureService.evaluate(
            Eithers.forRight3(measureDef.getMeasure()),
            periodStart,
            periodEnd,
            null,
            subject,
            null,
            null,
            terminologyEndpoint,
            null,
            additionalData,
            null,
            null,
            null);
  }
}
