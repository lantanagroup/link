package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.*;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FixResourceId implements IReportGenerationEvent, IReportGenerationDataEvent {

  private static final Logger logger = LoggerFactory.getLogger(FixResourceId.class);

  public void execute(ReportCriteria reportCriteria, ReportContext context) {
  }

  public void execute(Bundle data, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
    logger.info("Called: " + FixResourceId.class.getName());
    ResourceIdChanger.changeIds(data);
  }

  public void execute(List<DomainResource> resourceList, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
    throw new RuntimeException("Not Implemented yet.");
  }
}


