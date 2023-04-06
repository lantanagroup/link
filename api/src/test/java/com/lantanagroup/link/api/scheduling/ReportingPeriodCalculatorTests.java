package com.lantanagroup.link.api.scheduling;

import com.lantanagroup.link.ReportingPeriodCalculator;
import com.lantanagroup.link.ReportingPeriodMethods;
import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class ReportingPeriodCalculatorTests {
  private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  @Test
  public void testLastMonth() throws ParseException {
    ReportingPeriodCalculator calculator = new ReportingPeriodCalculator(ReportingPeriodMethods.LastMonth);

    calculator.setBaseDate(dateFormat.parse("2023-03-21 14:31:34"));
    String startDate = calculator.getStart();
    Assert.assertEquals("2023-02-01T00:00:00.000Z", startDate);
    String endDate = calculator.getEnd();
    Assert.assertEquals("2023-02-28T23:59:59.000Z", endDate);

    calculator.setBaseDate(dateFormat.parse("2023-04-21 14:31:34"));
    startDate = calculator.getStart();
    Assert.assertEquals("2023-03-01T00:00:00.000Z", startDate);
    endDate = calculator.getEnd();
    Assert.assertEquals("2023-03-31T23:59:59.000Z", endDate);
  }

  @Test
  public void testCurrentMonth() throws ParseException {
    ReportingPeriodCalculator calculator = new ReportingPeriodCalculator(ReportingPeriodMethods.CurrentMonth);

    calculator.setBaseDate(dateFormat.parse("2023-03-21 14:31:34"));
    String startDate = calculator.getStart();
    Assert.assertEquals("2023-03-01T00:00:00.000Z", startDate);
    String endDate = calculator.getEnd();
    Assert.assertEquals("2023-03-31T23:59:59.000Z", endDate);

    calculator.setBaseDate(dateFormat.parse("2023-04-21 14:31:34"));
    startDate = calculator.getStart();
    Assert.assertEquals("2023-04-01T00:00:00.000Z", startDate);
    endDate = calculator.getEnd();
    Assert.assertEquals("2023-04-30T23:59:59.000Z", endDate);
  }

  @Test
  public void testLastWeek() throws ParseException {
    ReportingPeriodCalculator calculator = new ReportingPeriodCalculator(ReportingPeriodMethods.LastWeek);

    calculator.setBaseDate(dateFormat.parse("2023-03-21 14:31:34"));
    String startDate = calculator.getStart();
    Assert.assertEquals("2023-03-12T00:00:00.000Z", startDate);
    String endDate = calculator.getEnd();
    Assert.assertEquals("2023-03-18T23:59:59.000Z", endDate);

    calculator.setBaseDate(dateFormat.parse("2023-04-21 14:31:34"));
    startDate = calculator.getStart();
    Assert.assertEquals("2023-04-09T00:00:00.000Z", startDate);
    endDate = calculator.getEnd();
    Assert.assertEquals("2023-04-15T23:59:59.000Z", endDate);
  }

  @Test
  public void testCurrentWeek() throws ParseException {
    ReportingPeriodCalculator calculator = new ReportingPeriodCalculator(ReportingPeriodMethods.CurrentWeek);

    calculator.setBaseDate(dateFormat.parse("2023-03-21 14:31:34"));
    String startDate = calculator.getStart();
    Assert.assertEquals("2023-03-19T00:00:00.000Z", startDate);
    String endDate = calculator.getEnd();
    Assert.assertEquals("2023-03-25T23:59:59.000Z", endDate);

    calculator.setBaseDate(dateFormat.parse("2023-04-21 14:31:34"));
    startDate = calculator.getStart();
    Assert.assertEquals("2023-04-16T00:00:00.000Z", startDate);
    endDate = calculator.getEnd();
    Assert.assertEquals("2023-04-22T23:59:59.000Z", endDate);
  }
}
