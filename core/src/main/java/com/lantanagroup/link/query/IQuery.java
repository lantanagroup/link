package com.lantanagroup.link.query;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.springframework.context.ApplicationContext;

import java.util.List;

public interface IQuery {
  void execute(ReportCriteria criteria, ReportContext context, QueryPhase queryPhase);

  void setApplicationContext(ApplicationContext context);

  IGenericClient getFhirQueryClient();
}
