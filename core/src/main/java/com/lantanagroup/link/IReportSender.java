package com.lantanagroup.link;

import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.db.model.Report;

import javax.servlet.http.HttpServletRequest;

@SuppressWarnings("unused")
public interface IReportSender {
  void send(Report report, HttpServletRequest request, LinkCredentials user) throws Exception;
}
