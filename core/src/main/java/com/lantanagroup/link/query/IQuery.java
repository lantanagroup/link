package com.lantanagroup.link.query;

import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.QueryResponse;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.context.ApplicationContext;

import java.util.List;

public interface IQuery {
  List<QueryResponse>  execute(List<PatientOfInterestModel> patientIdentifiers, List<String> resourceTypes);
  void setApplicationContext(ApplicationContext context);
}
