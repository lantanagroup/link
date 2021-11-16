package com.lantanagroup.link.thsa;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.IReportSender;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.r4.model.MeasureReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;

public class THSASender implements IReportSender {
  protected static final Logger logger = LoggerFactory.getLogger(THSASender.class);

  @Override
  public void send(MeasureReport report, HttpServletRequest request, Authentication auth, FhirDataProvider fhirDataProvider) throws Exception {
    throw new NotImplementedException("Not yet implemented");
  }
}
