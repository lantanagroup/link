package com.lantanagroup.link.api.scheduling;

import com.lantanagroup.link.config.scheduling.ReportingPeriodMethods;
import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class ReportingPeriodCalculatorTests {
  private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  @Test
  public void testLastMonthStartDate() throws ParseException {
    ReportingPeriodCalculator calculator = new ReportingPeriodCalculator(ReportingPeriodMethods.LastMonth);

    calculator.setBaseDate(dateFormat.parse("2023-03-21 14:31:34"));
    String startDate = calculator.getStart();
    Assert.assertEquals("2023-02-01T00:00:00.000Z", startDate);

    calculator.setBaseDate(dateFormat.parse("2023-04-21 14:31:34"));
    startDate = calculator.getStart();
    Assert.assertEquals("2023-03-01T00:00:00.000Z", startDate);
  }

  @Test
  public void testLastMonthEndDate() throws ParseException {
    ReportingPeriodCalculator calculator = new ReportingPeriodCalculator(ReportingPeriodMethods.LastMonth);

    calculator.setBaseDate(dateFormat.parse("2023-03-21 14:31:34"));
    String endDate = calculator.getEnd();
    Assert.assertEquals("2023-02-28T23:59:59.000Z", endDate);

    calculator.setBaseDate(dateFormat.parse("2023-04-21 14:31:34"));
    endDate = calculator.getEnd();
    Assert.assertEquals("2023-03-31T23:59:59.000Z", endDate);
  }
}
