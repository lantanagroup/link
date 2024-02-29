package com.lantanagroup.link;

import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;

public interface IPatientIdProvider {
  void loadPatientsOfInterest(TenantService tenantService, ReportCriteria criteria, ReportContext context);
}
