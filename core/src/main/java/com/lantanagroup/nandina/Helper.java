package com.lantanagroup.nandina;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class Helper {
	
  private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	
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
    return simpleDateFormat.format(date);
  }
  
  public static Date parseFhirDate(String dateStr) throws ParseException {
	  return simpleDateFormat.parse(dateStr);
  }
}
