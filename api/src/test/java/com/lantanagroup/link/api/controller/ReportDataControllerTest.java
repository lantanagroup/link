package com.lantanagroup.link.api.controller;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReportDataControllerTest {

  @Ignore
  public void csvEndPoint() throws Exception {
    ReportDataController reportDataController = new ReportDataController();
    reportDataController.retrieveCSVData("csv content");
    reportDataController.retrieveCSVData("csv content");
  }
}