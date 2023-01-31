package com.lantanagroup.link;

import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;

import java.util.List;

public interface IReportGenerationDataEvent {

  void execute(Bundle data, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext);

  void execute(List<DomainResource> data, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext);

}
