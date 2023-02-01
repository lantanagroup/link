package com.lantanagroup.link.query;

import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.springframework.context.ApplicationContext;

import java.util.List;

public interface IQuery {
  // TODO: The measureIds parameter actually represents identifier values, not IDs
  //       It should probably be renamed, but that change will need to be propagated through all the query logic
  void execute(ReportCriteria criteria, ReportContext context, List<PatientOfInterestModel> patientIdentifiers, String reportId, List<String> resourceTypes, List<String> measureIds);

  void setApplicationContext(ApplicationContext context);
}
