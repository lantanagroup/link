package com.lantanagroup.link.db.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lantanagroup.link.model.ReportBase;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Report extends ReportBase {
  private ReportStatuses status = ReportStatuses.Draft;
  private String version = "0.1";
  private Date generatedTime;
  private Date submittedTime;
}
