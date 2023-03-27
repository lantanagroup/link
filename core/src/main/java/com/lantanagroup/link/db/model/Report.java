package com.lantanagroup.link.db.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
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
  private List<String> measureReports = new ArrayList<>();
}
