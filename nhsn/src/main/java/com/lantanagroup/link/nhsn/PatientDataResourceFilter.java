package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.IReportGenerationDataEvent;
import com.lantanagroup.link.config.query.USCoreConfig;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class PatientDataResourceFilter implements IReportGenerationDataEvent {
  private static final Logger logger = LoggerFactory.getLogger(PatientDataResourceFilter.class);

  @Autowired
  private USCoreConfig usCoreConfig;

  private boolean shouldRemove(ReportCriteria criteria, Resource resource) {
    switch (resource.getResourceType()) {
      case Condition:
        Condition condition = (Condition) resource;
        Instant start = Instant.parse(criteria.getPeriodStart()).minus(this.usCoreConfig.getLookbackPeriod());
        Instant end = Instant.parse(criteria.getPeriodEnd());

        if (condition.getOnsetDateTimeType() != null && condition.getOnsetDateTimeType().hasValue()) {
          Instant onset = condition.getOnsetDateTimeType().getValue().toInstant();
          boolean isAfter = onset.isAfter(start);
          boolean isBefore = onset.isBefore(end);
          boolean isWithin = isAfter && isBefore;

          if (!isWithin) {
            return true;
          }
        }
        break;
    }

    return false;
  }

  private String getPatientResourceIdFromBundle(Bundle bundle) {
    Optional<Resource> foundPatient = bundle.getEntry().stream()
            .filter(e -> e.getResource().getResourceType() == ResourceType.Patient)
            .map(e -> e.getResource())
            .findFirst();

    if (foundPatient.isPresent() && foundPatient.get().getIdElement() != null) {
      return foundPatient.get().getIdElement().getIdPart();
    }

    return "<unknown>";
  }

  @Override
  public void execute(Bundle bundle, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
    if (bundle == null || !bundle.hasEntry()) {
      logger.trace("Bundle data is null or empty");
      return;
    }

    if (this.usCoreConfig.getLookbackPeriod() == null) {
      logger.trace("US Core Config does not specify a lookback period");
      return;
    }

    String patientResourceId = this.getPatientResourceIdFromBundle(bundle);
    int total = bundle.getEntry().size();
    int filtered = 0;
    logger.info(String.format("Filtering patient data bundle for patient %s of %s resources", patientResourceId, total));

    // Loop through the entries in reverse so that we can easily remove entries without worrying about the index
    for (int i = bundle.getEntry().size() - 1; i >= 0; i--) {
      Bundle.BundleEntryComponent entry = bundle.getEntry().get(i);

      if (entry.getResource() != null) {
        if (this.shouldRemove(criteria, entry.getResource())) {
          bundle.getEntry().remove(i);
          filtered++;
        }
      }
    }

    logger.info(String.format("Filtered out %s resources of patient bundle %s for a total of %s", filtered, patientResourceId, bundle.getEntry().size()));
  }

  @Override
  public void execute(List<DomainResource> data, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
    throw new RuntimeException("Not yet implemented");
  }
}
