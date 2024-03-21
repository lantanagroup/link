package com.lantanagroup.link.model;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.db.model.PatientList;
import com.lantanagroup.link.db.model.tenant.QueryPlan;
import com.lantanagroup.link.query.QueryPhase;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
public class ReportContext {
  private volatile HttpServletRequest request;
  private volatile LinkCredentials user;
  private volatile String masterIdentifierValue;
  private volatile List<PatientList> patientLists = new ArrayList<>();
  private volatile List<PatientOfInterestModel> initialPatientsOfInterest = new ArrayList<>();
  private volatile List<MeasureContext> measureContexts = new ArrayList<>();
  private volatile QueryPlan queryPlan;
  private volatile IGenericClient client;
  private volatile List<String> debugPatients = new ArrayList<>();

  public ReportContext() {
  }

  public ReportContext(HttpServletRequest request, LinkCredentials user) {
    this.request = request;
    this.user = user;
  }

  public List<PatientOfInterestModel> getPatientsOfInterest() {
    return initialPatientsOfInterest;
  }

  public List<PatientOfInterestModel> getPatientsOfInterest(QueryPhase queryPhase) {
    switch (queryPhase) {
      case INITIAL:
        return initialPatientsOfInterest;
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
    private volatile String bundleId;
    private volatile Bundle reportDefBundle;
    private volatile Measure measure;
    private volatile String reportId;
    private volatile List<PatientOfInterestModel> initialPatientsOfInterest = new ArrayList<>();
    private volatile List<PatientOfInterestModel> supplementalPatientsOfInterest = Collections.synchronizedList(new ArrayList<>());
    private volatile MeasureReport measureReport;

    public List<PatientOfInterestModel> getPatientsOfInterest() {
      return initialPatientsOfInterest;
    }

    public List<PatientOfInterestModel> getPatientsOfInterest(QueryPhase queryPhase) {
      switch (queryPhase) {
        case INITIAL:
          return initialPatientsOfInterest;
        case SUPPLEMENTAL:
          return supplementalPatientsOfInterest;
        default:
          throw new IllegalArgumentException(queryPhase.toString());
      }
    }
  }
}
