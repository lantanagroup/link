package com.lantanagroup.link.query;

import com.lantanagroup.link.model.PatientOfInterestModel;
import org.springframework.context.ApplicationContext;

import java.util.List;

public interface IQuery {
  void execute(List<PatientOfInterestModel> patientIdentifiers, String reportId, List<String> resourceTypes, String measureId);

  void setApplicationContext(ApplicationContext context);
}
