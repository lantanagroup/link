package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.IPatientIdProvider;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.PatientList;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Component
public class StoredListProvider implements IPatientIdProvider {
  private static final Logger logger = LoggerFactory.getLogger(StoredListProvider.class);

  @Override
  public List<PatientOfInterestModel> getPatientsOfInterest(TenantService tenantService, ReportCriteria criteria, ReportContext context) {
    context.getPatientLists().clear();
    context.getPatientsOfInterest().clear();

    for (ReportContext.MeasureContext measureContext : context.getMeasureContexts()) {
      PatientList found = tenantService.findPatientList(criteria.getPeriodStart(), criteria.getPeriodEnd(), measureContext.getBundleId());

      if (found == null) {
        logger.warn("No patient census lists found");
        continue;
      }

      List<PatientOfInterestModel> patientsOfInterest = found.getPatients().stream().map((patientListId) -> {
        PatientOfInterestModel poi = new PatientOfInterestModel();
        if (StringUtils.isNotEmpty(patientListId.getReference())) {
          poi.setReference(patientListId.getReference());
        } else if (StringUtils.isNotEmpty(patientListId.getIdentifier())) {
          poi.setIdentifier(patientListId.getIdentifier());
        }
        return poi;
      }).collect(Collectors.toList());

      context.getPatientLists().add(found);
      measureContext.getPatientsOfInterest().addAll(patientsOfInterest);
      context.getPatientsOfInterest().addAll(patientsOfInterest);
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

    logger.info("Loaded {} patients from {} census lists", context.getPatientsOfInterest().size(), context.getPatientLists().size());
    return context.getPatientsOfInterest();
  }
}
