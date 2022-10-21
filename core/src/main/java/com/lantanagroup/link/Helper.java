package com.lantanagroup.link;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

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

  public static Date getStartOfDay(Date date) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return calendar.getTime();
  }

  public static Date getEndOfDay(Date date, int millisecond) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    calendar.set(Calendar.HOUR_OF_DAY, 23);
    calendar.set(Calendar.MINUTE, 59);
    calendar.set(Calendar.SECOND, 59);
    calendar.set(Calendar.MILLISECOND, millisecond);
    return calendar.getTime();
  }

  public static Date getStartOfMonth(Date date) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return calendar.getTime();
  }

  public static Date getEndOfMonth(Date date, int millisecond) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
    calendar.set(Calendar.HOUR_OF_DAY, 23);
    calendar.set(Calendar.MINUTE, 59);
    calendar.set(Calendar.SECOND, 59);
    calendar.set(Calendar.MILLISECOND, millisecond);
    return calendar.getTime();
  }

  public static String URLEncode(String url) {
    try {
      return URLEncoder.encode(url, "UTF-8").replace("+", "%20");
    } catch (Exception ex) {
      return url;
    }
  }

  public static <T> List<T> concatenate(List<T> list1, List<T> list2) {
    List<T> list = new ArrayList<>();
    list.addAll(list1);
    list.addAll(list2);
    return list;
  }

  public static boolean validateLoggerValue(String logValue) {
    //check for empty/null input
    if (StringUtils.isEmpty(logValue)) {
      return false;
    }

    String allowedLogCharacters = "^[\\w,\\s\\.-]+$";
    return Pattern.matches(allowedLogCharacters, logValue);
  }

  public static boolean validateHeaderValue(String headerValue) {
    //check for empty/null input
    if (StringUtils.isEmpty(headerValue)) {
      return false;
    }

    String allowedHeaderCharacters = "^[\\w,\\h\\.-]+$";
    return Pattern.matches(allowedHeaderCharacters, headerValue);
  }

  public static boolean validateApiKey(String apiKey) {
    //check for empty/null input
    if (StringUtils.isEmpty(apiKey)) {
      return false;
    }

    String allowedKeyCharacters = "^[\\S]+$";
    return Pattern.matches(allowedKeyCharacters, apiKey);
  }

  public static boolean validateBearerToken(String token) {
    //check for empty/null input
    if (StringUtils.isEmpty(token)) {
      return false;
    }

    String bearer = "[A-Za-z0-9\\-\\._~\\+\\/]+=*";
    return Pattern.matches(bearer, token);
  }

  public static String validateReportId(String reportId) throws Exception {
    //check for empty/null input
    if (StringUtils.isEmpty(reportId)) {
      throw new Exception("Invalid Report Id");
    }

    String safeString = "(?u)^[.\\\\w\\\\s*,()&+-]{0,1024}$";
    reportId = reportId.replace( '\n' ,  '_' )
            .replace( '\r' , '_' )
            .replace( '\t' , '_' )
            .replace('<', '_')
            .replace('>', '_');

    if(Pattern.matches(safeString, reportId)) {
      return StringEscapeUtils.escapeHtml4(reportId);
    }
    else {
      throw new Exception("Invalid Report Id");
    }
  }

  public static String cleanHeaderManipulationChars(String val) {
    //check for empty/null input
    if (StringUtils.isEmpty(val)) {
      return val;
    }

    String whiteList = "[^A-Za-z0-9\\-\\._~\\+\\/]";
    val = val.replaceAll(whiteList, "");

    //testing fortify logic
//    val = val.replace('\r', '_')
//            .replace('\n', '_')
//            .replace('\t', '_')
//            .replace('=', '_')
//            .replace(':', '_')
//            .replace('<', '_')
//            .replace('>', '_');

    return val;

  }

  public static String encodeLogging(String message) {
    if (StringUtils.isEmpty(message)) {
      return message;
    }

    //redundant checks to satisfy fortify scans
    message = message.replace('\n', '_').replace('\r', '_')
            .replace('\t', '_');

    String whiteList = "[^A-Za-z0-9\\-\\._~\\+\\/]";
    message = message.replaceAll(whiteList, "");

    message = quoteApostrophe(message);
    message = StringEscapeUtils.escapeHtml4(message);
    return message;
  }

  public static String encodeForUrl(String val) throws UnsupportedEncodingException, URISyntaxException {

    if (StringUtils.isEmpty(val)) {
      return val;
    }

    val = val.replace( '\n' ,  '_' ).replace( '\r' , '_' )
            .replace( '\t' , '_' );

//    URI requestURI = new URI(val);
//    String scheme = requestURI.getScheme();
//    String host = requestURI.getHost();
//    String path = requestURI.getPath();
//    String query = requestURI.getRawQuery();
//
//    val = scheme + "://" + host + path + URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
    return val;
  }

  public static String quoteApostrophe(String input) {
    if (!StringUtils.isEmpty(input))
      return input.replaceAll("[\']", "&rsquo;");
    else
      return null;
  }
}
