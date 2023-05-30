package com.lantanagroup.link.events;

import com.lantanagroup.link.IReportGenerationDataEvent;
import com.lantanagroup.link.PeriodDateFixer;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FixPeriodDates implements IReportGenerationDataEvent
{
  private static final Logger logger = LoggerFactory.getLogger(FixPeriodDates.class);

  public void execute(TenantService tenantService, Bundle data, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext)
  {
    logger.info("Called: " + FixPeriodDates.class.getName());
    PeriodDateFixer fixer = new PeriodDateFixer(data);
    fixer.FixDates();
  }

  public void execute(TenantService tenantService, List<DomainResource> resourceList, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
    throw new RuntimeException("Not Implemented yet.");
  }
}

