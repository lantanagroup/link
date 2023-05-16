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

  private static boolean isWithin(Instant target, Instant start, Instant end) {
    boolean isAfter = target.isAfter(start);
    boolean isBefore = target.isBefore(end);
    return isAfter && isBefore;
  }

  private static boolean periodOverlaps(Period period, Instant start, Instant end) {
    if (period.hasStart() && !period.hasEnd()) {
      Instant targetStart = period.getStart().toInstant();

      if (isWithin(targetStart, start, end)) {
        return true;
      }
    } else if (!period.hasStart() && period.hasEnd()) {
      Instant targetEnd = period.getEnd().toInstant();

      if (isWithin(targetEnd, start, end)) {
        return true;
      }
    } else if (period.hasStart() && period.hasEnd()) {
      Instant targetStart = period.getStart().toInstant();
      Instant targetEnd = period.getEnd().toInstant();

      boolean startsWithin = isWithin(targetStart, start, end);
      boolean endsWithin = isWithin(targetEnd, start, end);
      return startsWithin || endsWithin;
    } else {
      logger.error("Period does not have either a start or end date");
    }

    return false;
  }

  public static boolean shouldRemove(ReportCriteria criteria, java.time.Period lookBackPeriod, Resource resource) {
    Instant start = new DateTimeType(criteria.getPeriodStart()).getValue().toInstant().minus(lookBackPeriod);
    Instant end = new DateTimeType(criteria.getPeriodEnd()).getValue().toInstant();

    switch (resource.getResourceType()) {
      case Condition:
        Condition condition = (Condition) resource;

        if (condition.getOnset() != null) {
          if (condition.getOnset() instanceof DateTimeType) {
            Instant onset = condition.getOnsetDateTimeType().getValue().toInstant();

            if (!isWithin(onset, start, end)) {
              return true;
            }
          } else if (condition.getOnset() instanceof Period) {
            boolean overlaps = periodOverlaps(condition.getOnsetPeriod(), start, end);

            if (!overlaps) {
              return true;
            }
          }
        }
        break;
      case ServiceRequest:
        ServiceRequest serviceRequest = (ServiceRequest) resource;

        if (serviceRequest.getAuthoredOn() != null) {
          if (!isWithin(serviceRequest.getAuthoredOn().toInstant(), start, end)) {
            return true;
          }
        }
        break;
      case Specimen:
        Specimen specimen = (Specimen) resource;

        if (specimen.getReceivedTime() != null) {
          if (!isWithin(specimen.getReceivedTime().toInstant(), start, end)) {
            return true;
          }
        }
    }

    return false;
  }

  // TODO: This is always unknown if executed during AfterPatientResourceQuery
  //       In that case, the bundle argument is resource-type-specific, not an entire patient data bundle
  //       So it doesn't include the patient resource
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
    logger.info(String.format("Filtering bundle for patient %s of %s resources", patientResourceId, total));

    // Loop through the entries in reverse so that we can easily remove entries without worrying about the index
    for (int i = bundle.getEntry().size() - 1; i >= 0; i--) {
      Bundle.BundleEntryComponent entry = bundle.getEntry().get(i);

      if (entry.getResource() != null) {
        if (shouldRemove(criteria, this.usCoreConfig.getLookbackPeriod(), entry.getResource())) {
          bundle.getEntry().remove(i);
          filtered++;
        }
      }
    }

    logger.info(String.format("Filtered out %s resources of patient %s for a total of %s", filtered, patientResourceId, bundle.getEntry().size()));
  }

  @Override
  public void execute(List<DomainResource> data, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
    throw new RuntimeException("Not yet implemented");
  }
}
