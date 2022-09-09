package com.lantanagroup.link.query;

import com.lantanagroup.link.model.PatientOfInterestModel;
import org.springframework.context.ApplicationContext;

import java.util.List;

public interface IQuery {
  // TODO: The measureId parameter actually represents an identifier, not an ID
  //       It should probably be renamed, but that change will need to be propagated through all the query logic
  void execute(List<PatientOfInterestModel> patientIdentifiers, String reportId, List<String> resourceTypes, String measureId);

  void setApplicationContext(ApplicationContext context);
}
