package com.lantanagroup.link;

import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

public class Helper {
  public static final String SIMPLE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";

  public static boolean isNullOrEmpty(String value) {
    return value == null || value.isEmpty();
  }

  public static String getFhirDate() {
    return Helper.getFhirDate(new Date());
  }

  public static String getFhirDate(LocalDateTime localDateTime) {
    Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    Date date = Date.from(instant);
    return Helper.getFhirDate(date);
  }

  public static String getFhirDate(Date date) {
    return new SimpleDateFormat(SIMPLE_DATE_FORMAT).format(date);
  }

  public static Date parseFhirDate(String dateStr) throws ParseException {
    return new SimpleDateFormat(SIMPLE_DATE_FORMAT).parse(dateStr);
  }

  public static Date addDays(Date date, int numberOfDays) throws ParseException {
    Calendar c = Calendar.getInstance();
    c.setTime(date);
    c.add(Calendar.DATE, 1);  // number of days to add
    return c.getTime();
  }

  public static String URLEncode(String url) {
    try {
      return URLEncoder.encode(url, "UTF-8").replace("+", "%20");
    } catch (Exception ex) {
      return url;
    }
  }
}
