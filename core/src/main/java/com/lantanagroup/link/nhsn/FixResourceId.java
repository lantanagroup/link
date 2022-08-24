package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.IReportGenerationEvent;
import com.lantanagroup.link.ResourceIdChanger;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.model.ReportContext;
import com.lantanagroup.link.model.ReportCriteria;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FixResourceId implements IReportGenerationEvent {

  private static final Logger logger = LoggerFactory.getLogger(FixResourceId.class);

  public void execute(ReportCriteria reportCriteria, ReportContext context, ApiConfig config, FhirDataProvider fhirDataProvider) {

    // Fix resource IDs in the patient data bundle that are invalid (longer than 64 characters)
    // (note: this also fixes the references to resources within invalid ids)
    for (PatientOfInterestModel patientOfInterest : context.getPatientsOfInterest()) {
      logger.info("Patient is: " + patientOfInterest.getId());
      IBaseResource patientBundle = fhirDataProvider.getBundleById(context.getReportId() + "-" + patientOfInterest.getId().hashCode());
      ResourceIdChanger.changeIds((Bundle) patientBundle);
      fhirDataProvider.updateResource(patientBundle);
    }
  }
}


