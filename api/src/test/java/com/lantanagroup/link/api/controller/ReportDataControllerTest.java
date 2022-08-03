package com.lantanagroup.link.api.controller;

import org.junit.Ignore;

import java.nio.charset.StandardCharsets;

public class ReportDataControllerTest {

  @Ignore
  public void csvEndPoint() throws Exception {
    // TODO: Remove @Ignore and add unit testing logic
    // TODO: Mock out the ReportDataController so that the config can be set
    ReportDataController reportDataController = new ReportDataController();
    reportDataController.retrieveData("csv content".getBytes(StandardCharsets.UTF_8), "csv");
    reportDataController.retrieveData("csv content".getBytes(StandardCharsets.UTF_8), "csv");
  }
}
