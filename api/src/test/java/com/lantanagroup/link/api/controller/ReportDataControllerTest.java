package com.lantanagroup.link.api.controller;

import org.junit.Test;

import static org.junit.Assert.*;

public class ReportDataControllerTest {

  @Test
  public void csvEndPoint() throws Exception {
    ReportDataController reportDataController = new ReportDataController();
    reportDataController.retrieveCSVData("bed", "csv content");
    reportDataController.retrieveCSVData("ventilator", "csv content");
  }
}