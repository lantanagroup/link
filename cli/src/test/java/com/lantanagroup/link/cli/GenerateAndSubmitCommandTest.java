package com.lantanagroup.link.cli;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class GenerateAndSubmitCommandTest {

  @Test
  public void getStartDateTestAtStart() {
    int day = 1;
    int month = 1;
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    Calendar test = new GregorianCalendar();
    test.add(Calendar.HOUR, day * 24);
    test.add(Calendar.MONTH, month);
    test.set(Calendar.MILLISECOND, 0);
    test.set(Calendar.SECOND, 0);
    test.set(Calendar.MINUTE, 0);
    test.set(Calendar.HOUR_OF_DAY, 0);

    Date date = GenerateAndSubmitCommand.getStartDate(day, month, true);
    Assert.assertEquals(test.getTime(), date);
  }

  @Test
  public void getEndOfDayTest() {
    int day = 31;
    int month = 1;
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    Calendar test = new GregorianCalendar();
    test.add(Calendar.HOUR, day * 24);
    test.add(Calendar.MONTH, month);
    test.set(Calendar.HOUR_OF_DAY, 23);
    test.set(Calendar.MINUTE, 59);
    test.set(Calendar.SECOND, 59);
    test.set(Calendar.MILLISECOND, 0);

    Date date = GenerateAndSubmitCommand.getEndDate(day, month, true);
    Assert.assertEquals(test.getTime(), date);
  }
}
