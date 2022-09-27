package com.lantanagroup.link.nhsn;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.IPatientIdProvider;
import com.lantanagroup.link.IdentifierHelper;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ListResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class StoredListProvider implements IPatientIdProvider {
  private static final Logger logger = LoggerFactory.getLogger(StoredListProvider.class);

  @Override
  public List<PatientOfInterestModel> getPatientsOfInterest(ReportCriteria criteria, ReportContext context, ApiConfig config) {
    FhirContext ctx = FhirContextProvider.getFhirContext();
    context.getPatientCensusLists().clear();
    context.getPatientsOfInterest().clear();

    for (ReportContext.MeasureContext measureContext : context.getMeasureContexts()) {
      Identifier reportDefIdentifier = measureContext.getReportDefBundle().getIdentifier();
      String system = reportDefIdentifier.getSystem();
      String value = reportDefIdentifier.getValue();

      logger.info("Searching for patient census lists with identifier {}|{} and applicable period {}-{}", system, value, criteria.getPeriodStart(), criteria.getPeriodEnd());
      Bundle bundle = context.getFhirProvider().findListByIdentifierAndDate(system, value, criteria.getPeriodStart(), criteria.getPeriodEnd());
      if (bundle.getEntry().size() == 0) {
        logger.warn("No patient census lists found");
        continue;
      }

      List<ListResource> lists = FhirHelper.getAllPages(bundle, context.getFhirProvider(), ctx, ListResource.class);
      for (ListResource list : lists) {
        List<PatientOfInterestModel> pois = list.getEntry().stream().map(patient -> {

          PatientOfInterestModel poi = new PatientOfInterestModel();
          if (patient.getItem().getIdentifier() != null) {
            poi.setIdentifier(IdentifierHelper.toString(patient.getItem().getIdentifier()));
          }
          if (patient.getItem().getReference() != null) {
            poi.setReference(patient.getItem().getReference());
          }
          return poi;
        }).collect(Collectors.toList());

        measureContext.getPatientsOfInterest().addAll(pois);
        context.getPatientCensusLists().add(list);
        context.getPatientsOfInterest().addAll(pois);
      }
    }

    // Deduplicate POIs, ensuring that ReportContext and MeasureContext POI lists refer to the same objects
    Collector<PatientOfInterestModel, ?, Map<String, PatientOfInterestModel>> deduplicator =
            Collectors.toMap(PatientOfInterestModel::toString, Function.identity(), (poi1, poi2) -> poi1);
    Map<String, PatientOfInterestModel> poiMap = context.getPatientsOfInterest().stream().collect(deduplicator);
    context.setPatientsOfInterest(new ArrayList<>(poiMap.values()));
    for (ReportContext.MeasureContext measureContext : context.getMeasureContexts()) {
      measureContext.setPatientsOfInterest(measureContext.getPatientsOfInterest().stream()
              .collect(deduplicator)
              .values().stream()
              .map(poi -> poiMap.get(poi.toString()))
              .collect(Collectors.toList()));
    }

    logger.info("Loaded {} patients from {} census lists", context.getPatientsOfInterest().size(), context.getPatientCensusLists().size());
    return context.getPatientsOfInterest();
  }
}
