package com.lantanagroup.link.query;

import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.QueryResponse;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Measure;
import org.springframework.context.ApplicationContext;

import java.util.List;

public interface IQuery {
  void  execute(List<PatientOfInterestModel> patientIdentifiers, String reportId, List<String> resourceTypes, String measureId);
  void setApplicationContext(ApplicationContext context);
}
