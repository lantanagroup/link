package com.lantanagroup.link.thsa;

import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.GenericSender;
import com.lantanagroup.link.IReportSender;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.MeasureReport;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class MeasureReportSender extends GenericSender implements IReportSender {
  @Override
  public void send(MeasureReport masterMeasureReport, DocumentReference documentReference, HttpServletRequest request, Authentication auth, FhirDataProvider fhirDataProvider, Boolean sendWholeBundle, boolean removeGeneratedObservations) throws Exception {
    this.sendContent(masterMeasureReport, documentReference, fhirDataProvider);
  }

  @Override
  public String bundle(Bundle bundle, FhirDataProvider fhirProvider, String type) {
    return null;
  }
}
