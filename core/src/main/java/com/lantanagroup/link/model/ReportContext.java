package com.lantanagroup.link.model;

import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.db.model.PatientList;
import com.lantanagroup.link.db.model.tenant.QueryPlan;
import com.lantanagroup.link.query.QueryPhase;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
public class ReportContext {
  private HttpServletRequest request;
  private LinkCredentials user;
  private String masterIdentifierValue;
  private List<PatientList> patientLists = new ArrayList<>();
  private List<PatientOfInterestModel> patientsOfInterest = new ArrayList<>();
  private List<MeasureContext> measureContexts = new ArrayList<>();
  private QueryPlan queryPlan;
  private List<String> debugPatients = new ArrayList<>();

  public ReportContext() {
  }

  public ReportContext(HttpServletRequest request, LinkCredentials user) {
    this.request = request;
    this.user = user;
  }

  public List<PatientOfInterestModel> getPatientsOfInterest(QueryPhase queryPhase) {
    switch (queryPhase) {
      case INITIAL:
        return patientsOfInterest;
      case SUPPLEMENTAL:
        return measureContexts.stream()
                .flatMap(measureContext -> measureContext.getPatientsOfInterest(queryPhase).stream())
                .distinct()
                .collect(Collectors.toList());
      default:
        throw new IllegalArgumentException(queryPhase.toString());
    }
  }

  @Getter
  @Setter
  public static class MeasureContext {
    private String bundleId;
    private Bundle reportDefBundle;
    private Measure measure;
    private String reportId;
    private List<PatientOfInterestModel> patientsOfInterest = new ArrayList<>();
    private Map<String, MeasureReport> patientReportsByPatientId = new HashMap<>();
    private MeasureReport measureReport;

    public List<PatientOfInterestModel> getPatientsOfInterest(QueryPhase queryPhase) {
      switch (queryPhase) {
        case INITIAL:
          return patientsOfInterest;
        case SUPPLEMENTAL:
          Set<String> patientIds = patientReportsByPatientId.entrySet().stream()
                  .filter(reportById -> FhirHelper.hasNonzeroPopulationCount(reportById.getValue()))
                  .map(Map.Entry::getKey)
                  .collect(Collectors.toSet());
          return patientsOfInterest.stream()
                  .filter(poi -> patientIds.contains(poi.getId()))
                  .collect(Collectors.toList());
        default:
          throw new IllegalArgumentException(queryPhase.toString());
      }
    }

    public Collection<MeasureReport> getPatientReports() {
      return patientReportsByPatientId.values();
    }
  }
}
