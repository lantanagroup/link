package com.lantanagroup.link;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.lantanagroup.link.model.ApiInfoModel;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private static final Logger logger = LoggerFactory.getLogger(Helper.class);

  public static final String SIMPLE_DATE_MILLIS_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
  public static final String SIMPLE_DATE_SECONDS_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
  public static final String RFC_1123_DATE_TIME_FORMAT = "EEE, d MMM yyyy HH:mm:ss Z";

  public static ApiInfoModel getVersionInfo(String evaluationService) {
    String cqfVersion = null;

    if (StringUtils.isNotEmpty(evaluationService)) {
      CapabilityStatement cs = new FhirDataProvider(evaluationService)
              .getClient()
              .capabilities()
              .ofType(CapabilityStatement.class)
              .execute();

      if (cs != null) {
        cqfVersion = cs.getImplementation().getDescription();
      } else {
        logger.warn("Could not retrieve capabilities of evaluation service, using cqf-version specified in build.yml");
      }
    }

    try {
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

      URL buildFile = Helper.class.getClassLoader().getResource("build.yml");

      if (buildFile == null) {
        logger.warn("No build.yml file found, returning default \"dev\" build and \"0.9.0\" version");
        return new ApiInfoModel(cqfVersion);
      }

      ApiInfoModel apiInfo = mapper.readValue(buildFile, ApiInfoModel.class);

      if (StringUtils.isNotEmpty(cqfVersion)) {
        apiInfo.setCqfVersion(cqfVersion);
      }

      return apiInfo;
    } catch (IOException ex) {
      logger.error("Error deserializing build.yml file", ex);
      return new ApiInfoModel(cqfVersion);
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

  public static String quoteApostrophe(String input) {
    if (!StringUtils.isEmpty(input))
      return input.replaceAll("[\']", "&rsquo;");
    else
      return null;
  }
}
