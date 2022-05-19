package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.IReportGenerationEvent;
import com.lantanagroup.link.ResourceIdChanger;
import com.lantanagroup.link.model.QueryResponse;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;

public class FixResourceId implements IReportGenerationEvent {

  public void execute(ReportCriteria reportCriteria, ReportContext context) {

    // Fix resource IDs in the patient data bundle that are invalid (longer than 64 characters)
    // (note: this also fixes the references to resources within invalid ids)
    for (QueryResponse patientQueryResponse : context.getPatientData())
      ResourceIdChanger.changeIds(patientQueryResponse.getBundle());
  }
}


