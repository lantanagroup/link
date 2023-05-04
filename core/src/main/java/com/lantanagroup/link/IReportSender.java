package com.lantanagroup.link;

import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.Report;
import org.hl7.fhir.r4.model.Bundle;

import javax.servlet.http.HttpServletRequest;

@SuppressWarnings("unused")
public interface IReportSender {
  void send(TenantService tenantService, Bundle submissionBundle, Report reoprt, HttpServletRequest request, LinkCredentials user) throws Exception;
}
