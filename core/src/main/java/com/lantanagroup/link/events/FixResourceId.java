package com.lantanagroup.link.events;

import com.lantanagroup.link.IReportGenerationDataEvent;
import com.lantanagroup.link.ResourceIdChanger;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Looks for resources that have an ID with more than 64 characters and replaces it with a hash of the ID. Updates references to the old ID to use the new hashed ID.
 */
public class FixResourceId implements IReportGenerationDataEvent {

  private static final Logger logger = LoggerFactory.getLogger(FixResourceId.class);

  public void execute(TenantService tenantService, Bundle data, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
    logger.info("Called: " + FixResourceId.class.getName());
    ResourceIdChanger.changeIds(data);
  }

  public void execute(TenantService tenantService, List<DomainResource> resourceList, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
    throw new RuntimeException("Not Implemented yet.");
  }
}