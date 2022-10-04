package com.lantanagroup.link;

import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Resource;

import java.util.Collection;
import java.util.List;

public interface IReportGenerationDataEvent {

  void execute(Bundle data);

  void execute(List<DomainResource> data);

  default void execute(Bundle data, ReportCriteria criteria, ReportContext context) {
    execute(data);
  }

  default void execute(Bundle data, ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
    execute(data);
  }

  default void execute(List<DomainResource> data, ReportCriteria criteria, ReportContext context) {
    execute(data);
  }

  default void execute(List<DomainResource> data , ReportCriteria criteria, ReportContext context, ReportContext.MeasureContext measureContext) {
    execute(data);
  }

}
