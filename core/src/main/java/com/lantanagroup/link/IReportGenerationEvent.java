package com.lantanagroup.link;

import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;

public interface IReportGenerationEvent {

  public void execute(ReportCriteria reportCriteria, ReportContext context);
}
