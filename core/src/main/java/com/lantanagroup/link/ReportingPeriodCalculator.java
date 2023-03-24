package com.lantanagroup.link;

import com.lantanagroup.link.config.scheduling.ReportingPeriodMethods;
import lombok.Setter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class ReportingPeriodCalculator {
  private ReportingPeriodMethods method;
  private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

  @Setter
  private Date baseDate = new Date();

  public ReportingPeriodCalculator(ReportingPeriodMethods method) {
    this.method = method;
  }

  public String getStart() {
    Calendar cal = Calendar.getInstance();
    cal.setTime(this.baseDate);

    int startOfWeek = cal.get(Calendar.DAY_OF_WEEK) - cal.getFirstDayOfWeek();

    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    switch (this.method) {
      case LastMonth:
        cal.add(Calendar.MONTH, -1);
        cal.set(Calendar.DATE, 1);
        break;
      case LastWeek:
        cal.add(Calendar.DATE, -startOfWeek - 7);
        break;
      case CurrentMonth:
        cal.set(Calendar.DATE, 1);
        break;
      case CurrentWeek:
        cal.add(Calendar.DATE, -startOfWeek);
      default:
        throw new IllegalArgumentException("method");
    }

    return dateFormat.format(cal.getTime());
  }

  public String getEnd() {
    Calendar cal = Calendar.getInstance();
    cal.setTime(this.baseDate);

    int startOfWeek = cal.get(Calendar.DAY_OF_WEEK) - cal.getFirstDayOfWeek();

    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    switch (this.method) {
      case LastMonth:
        cal.set(Calendar.DATE, 1);
        cal.add(Calendar.SECOND, -1);
        break;
      default:
        throw new IllegalArgumentException("method");
    }

    return dateFormat.format(cal.getTime());
  }
}
