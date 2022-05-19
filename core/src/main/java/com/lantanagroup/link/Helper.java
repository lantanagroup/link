package com.lantanagroup.link;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class Helper {
  public static final String SIMPLE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
  public static final String SIMPLE_DATE_MILLIS_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
  public static final String SIMPLE_DATE_SECONDS_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
  public static final String RFC_1123_DATE_TIME_FORMAT = "EEE, d MMM yyyy HH:mm:ss Z";

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
    return new SimpleDateFormat(SIMPLE_DATE_MILLIS_FORMAT).format(date);
  }

  public static Date parseFhirDate(String dateStr) throws ParseException {
    SimpleDateFormat formatterMillis = new SimpleDateFormat(SIMPLE_DATE_MILLIS_FORMAT);
    SimpleDateFormat formatterSec = new SimpleDateFormat(SIMPLE_DATE_SECONDS_FORMAT);
    Date dateReturned;
    try {
      dateReturned = formatterMillis.parse(dateStr);
    } catch (Exception ex) {
      dateReturned = formatterSec.parse(dateStr);
    }
    return dateReturned;
  }

  public static Date addDays(Date date, int numberOfDays) throws ParseException {
    Calendar c = Calendar.getInstance();
    c.setTime(date);
    c.add(Calendar.DATE, 1);  // number of days to add
    return c.getTime();
  }

  public static String getEndOfDayDate(String date) throws ParseException {
    SimpleDateFormat sdf = new SimpleDateFormat(SIMPLE_DATE_MILLIS_FORMAT);
    return sdf.format(setEndOfDay(date));
  }

  public static Date setEndOfDay(String date) throws ParseException {

    SimpleDateFormat sdf = new SimpleDateFormat(SIMPLE_DATE_MILLIS_FORMAT);
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(sdf.parse(date));
    calendar.set(Calendar.HOUR_OF_DAY, 23);
    calendar.set(Calendar.MINUTE, 59);
    calendar.set(Calendar.SECOND, 59);
    calendar.set(Calendar.MILLISECOND, 999);
    return calendar.getTime();

  }

  public static String URLEncode(String url) {
    try {
      return URLEncoder.encode(url, "UTF-8").replace("+", "%20");
    } catch (Exception ex) {
      return url;
    }
  }

  public static List concatenate(List list1, List list2) {
    List<String> list = new ArrayList<>();
    list.addAll(list1);
    list.addAll(list2);
    return list;
  }
}
