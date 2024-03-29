package com.lantanagroup.link.model;

import com.lantanagroup.link.db.model.Report;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GlobalReportResponse extends Report {
  private String tenantId;
  private String tenantName;
  private String cdcOrgId;
  private Integer totalPatients;
  private Integer maxTotalInIP;
}
