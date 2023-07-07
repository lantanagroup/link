package com.lantanagroup.link;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.lantanagroup.link.model.ApiInfoModel;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Helper {
  public static final String SIMPLE_DATE_MILLIS_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
  public static final String SIMPLE_DATE_SECONDS_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
  public static final String RFC_1123_DATE_TIME_FORMAT = "EEE, d MMM yyyy HH:mm:ss Z";

  public static ApiInfoModel getVersionInfo(){
    try {
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      URL buildFile = Helper.class.getClassLoader().getResource("build.yml");

      if (buildFile == null) return new ApiInfoModel("dev", "0.9.0");

      return mapper.readValue(buildFile, ApiInfoModel.class);
    } catch (IOException ex) {
      return new ApiInfoModel("dev", "0.9.0");
    }
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

  public static <T> List<T> concatenate(List<T> list1, List<T> list2) {
    List<T> list = new ArrayList<>();
    list.addAll(list1);
    list.addAll(list2);
    return list;
  }

  public static String sanitizeString(String value) {
    if (StringUtils.isEmpty(value)) {
      return value;
    }

    //redundant checks to satisfy fortify scans
    value = value.replace('\n', '_').replace('\r', '_')
            .replace('\t', '_');

    String whiteList = "[^A-Za-z0-9\\-\\._~\\+\\/]";
    value = value.replaceAll(whiteList, "");

    value = quoteApostrophe(value);
    value = StringEscapeUtils.escapeHtml4(value);
    return value;
  }

  public static String quoteApostrophe(String input) {
    if (!StringUtils.isEmpty(input))
      return input.replaceAll("[\']", "&rsquo;");
    else
      return null;
  }
}
