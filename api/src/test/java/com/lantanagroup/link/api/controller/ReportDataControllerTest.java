package com.lantanagroup.link.api.controller;

import org.junit.Test;

import static org.junit.Assert.*;

public class ReportDataControllerTest {

  @Test
  public void csvEndPoint() {
    ReportDataController reportDataController = new ReportDataController();
    reportDataController.csvEndPoint("bed");
    reportDataController.csvEndPoint("ventilator");
  }
}