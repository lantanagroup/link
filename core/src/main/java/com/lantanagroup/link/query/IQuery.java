package com.lantanagroup.link.query;

import com.lantanagroup.link.model.PatientOfInterestModel;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.context.ApplicationContext;

import java.util.List;

public interface IQuery {
  Bundle execute(List<PatientOfInterestModel> patientIdentifiers);
  void setApplicationContext(ApplicationContext context);
}
