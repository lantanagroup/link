package com.lantanagroup.link.thsa;

import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.GenericSender;
import com.lantanagroup.link.IReportSender;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.bundler.BundlerConfig;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.MeasureReport;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Component
public class MeasureReportSender extends GenericSender implements IReportSender {
  @Override
  public void send(List<MeasureReport> masterMeasureReports, DocumentReference documentReference, HttpServletRequest request, LinkCredentials user, FhirDataProvider fhirDataProvider, BundlerConfig bundlerConfig) throws Exception {
    if (masterMeasureReports.size() == 1) {
      this.sendContent(masterMeasureReports.get(0), documentReference, fhirDataProvider);
    } else {
      Bundle bundle = new Bundle();
      bundle.setType(Bundle.BundleType.COLLECTION);
      for (MeasureReport masterMeasureReport : masterMeasureReports) {
        bundle.addEntry().setResource(masterMeasureReport);
      }
      this.sendContent(bundle, documentReference, fhirDataProvider);
    }
  }

  @Override
  public String bundle(Bundle bundle, FhirDataProvider fhirProvider, String type) {
    return null;
  }
}
