package com.lantanagroup.link;

import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;

public interface IReportGenerationEvent {
  void execute(TenantService tenantService, ReportCriteria reportCriteria, ReportContext context);

  default void execute(TenantService tenantService, ReportCriteria reportCriteria, ReportContext reportContext, ReportContext.MeasureContext measureContext) {
    execute(tenantService, reportCriteria, reportContext);
  }
}
