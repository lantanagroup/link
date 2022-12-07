package com.lantanagroup.link;

import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;

public interface IReportGenerationEvent {
  void execute(ReportCriteria reportCriteria, ReportContext context);

  default void execute(ReportCriteria reportCriteria, ReportContext reportContext, ReportContext.MeasureContext measureContext) {
    execute(reportCriteria, reportContext);
  }
}
