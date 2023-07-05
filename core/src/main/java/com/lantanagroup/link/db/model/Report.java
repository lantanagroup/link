package com.lantanagroup.link.db.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lantanagroup.link.model.ReportBase;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Report extends ReportBase {
  private ReportStatuses status = ReportStatuses.Draft;
  private Date submittedTime;
  private Date generatedTime = new Date();
  private String version = "0.1";
  private List<UUID> patientLists = new ArrayList<>();
  private List<String> aggregates = new ArrayList<>();
}
