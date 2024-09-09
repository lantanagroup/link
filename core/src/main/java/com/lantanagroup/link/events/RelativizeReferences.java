package com.lantanagroup.link.events;

import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.IReportGenerationDataEvent;
import com.lantanagroup.link.ReferenceRelativizer;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;

import java.util.List;

public class RelativizeReferences implements IReportGenerationDataEvent {
  @Override
  public void execute(TenantService tenantService, Bundle data, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
    ReferenceRelativizer relativizer = new ReferenceRelativizer(FhirContextProvider.getFhirContext());
    String baseUrl = tenantService.getConfig().getFhirQuery().getFhirServerBase();
    relativizer.relativize(data, baseUrl);
  }

  @Override
  public void execute(TenantService tenantService, List<DomainResource> data, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
  }
}
