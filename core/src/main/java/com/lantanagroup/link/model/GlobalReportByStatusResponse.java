package com.lantanagroup.link.model;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GlobalReportByStatusResponse {
  private List<GlobalReportResponse> completedReports;
  private List<GlobalReportResponse> failedReports;
  private List<GlobalReportResponse> pendingReports;
}