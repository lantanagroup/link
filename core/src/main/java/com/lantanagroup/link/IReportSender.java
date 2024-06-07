package com.lantanagroup.link;

import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.Report;

import jakarta.servlet.http.HttpServletRequest;

@SuppressWarnings("unused")
public interface IReportSender {
  void send(EventService eventService, TenantService tenantService, Report report, HttpServletRequest request, LinkCredentials user) throws Exception;
}
