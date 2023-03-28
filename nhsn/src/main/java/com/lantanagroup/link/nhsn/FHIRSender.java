package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.GenericSender;
import com.lantanagroup.link.IReportSender;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.db.model.Report;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;


@Component
public class FHIRSender extends GenericSender implements IReportSender {
  @Override
  public void send(Report report, HttpServletRequest request, LinkCredentials user) throws Exception {
    Bundle bundle = this.generateBundle(report);
    this.sendContent(bundle, report);
  }
}
