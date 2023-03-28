package com.lantanagroup.link.db.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.MeasureReport;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Report {
  private String id;
  private List<String> measureIds = new ArrayList<>();
  private String periodStart;
  private String periodEnd;
  private ReportStatuses status = ReportStatuses.Draft;
  private Date submittedTime;
  private Date generatedTime = new Date();
  private String version = "0.1";
  private List<PatientList> patientLists = new ArrayList<>();
  private List<MeasureReport> aggregates = new ArrayList<>();
}
