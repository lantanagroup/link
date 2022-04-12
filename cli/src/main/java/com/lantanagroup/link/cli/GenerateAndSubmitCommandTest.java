package com.lantanagroup.link.cli;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

public class GenerateAndSubmitCommandTest {

  @Test
  public void generateAndSubmitTest() {

    GenerateAndSubmitCommand generateAndSubmitCommand = new GenerateAndSubmitCommand();
    generateAndSubmitCommand.setConfigInfo(new GenerateAndSubmitConfig());
    generateAndSubmitCommand.getConfigInfo().setReportTypeId("Measure");

    generateAndSubmitCommand.getConfigInfo().setPeriodStart(new GenerateAndSubmitPeriodStart());
    generateAndSubmitCommand.getConfigInfo().getPeriodStart().setStartOfDay(true);


    generateAndSubmitCommand.getConfigInfo().setPeriodEnd(new GenerateAndSubmitPeriodEnd());
    generateAndSubmitCommand.getConfigInfo().getPeriodEnd().setEndOfDay(true);


    generateAndSubmitCommand.generateAndSubmit();
  }

  @Test
  public void getStartDateTest() {
    Date date;
  }

  @Test
  public void getEndDateTest() {
    Date date;
  }
}