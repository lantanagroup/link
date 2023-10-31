package com.lantanagroup.link;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.Aggregate;
import com.lantanagroup.link.db.model.Report;
import com.lantanagroup.link.model.ApiVersionModel;
import com.microsoft.sqlserver.jdbc.SQLServerDriver;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class Helper {
  private static final Logger logger = LoggerFactory.getLogger(Helper.class);

  public static final String SIMPLE_DATE_MILLIS_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
  public static final String SIMPLE_DATE_SECONDS_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
  public static final String RFC_1123_DATE_TIME_FORMAT = "EEE, d MMM yyyy HH:mm:ss Z";

  public static ApiVersionModel getVersionInfo(String evaluationService) {
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
        return new ApiVersionModel(cqfVersion);
      }

      ApiVersionModel apiInfo = mapper.readValue(buildFile, ApiVersionModel.class);

      if (StringUtils.isNotEmpty(cqfVersion)) {
        apiInfo.setCqfVersion(cqfVersion);
      }

      return apiInfo;
    } catch (IOException ex) {
      logger.error("Error deserializing build.yml file", ex);
      return new ApiVersionModel(cqfVersion);
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
      return "";
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

  public static String sanitizeUrl(String value) {
    if (StringUtils.isEmpty(value)) {
      return value;
    }

    //redundant checks to satisfy fortify scans
    value = value.replace('\n', '_').replace('\r', '_')
            .replace('\t', '_');

    String whiteList = "[^:/?#\\[\\]@!$&'()*+,;=A-Za-z0-9-_.~]";
    return value.replaceAll(whiteList, "");
  }

  public static String sanitizeHeader(String value) {
    return value.replaceAll("[^\\u0020-\\u007e]", "");
  }

  public static String quoteApostrophe(String input) {
    if (!StringUtils.isEmpty(input))
      return input.replaceAll("[\']", "&rsquo;");
    else
      return null;
  }

  public static String readInputStream(InputStream is) throws IOException {
    Reader inputStreamReader = new InputStreamReader(is);
    StringBuilder sb = new StringBuilder();

    int data = inputStreamReader.read();
    while (data != -1) {
      sb.append((char) data);
      data = inputStreamReader.read();
    }

    inputStreamReader.close();

    return sb.toString();
  }

  public static String getDatabaseName(String connectionString) {
    Driver driver = new SQLServerDriver();
    try {
      DriverPropertyInfo[] properties = driver.getPropertyInfo(connectionString, null);
      return Arrays.stream(properties)
              .filter(property -> property.name.equals("databaseName"))
              .findFirst()
              .map(property -> StringUtils.isNotEmpty(property.value) ? property.value : null)
              .orElse(null);
    } catch (SQLException e) {
      logger.warn("Parsing database connection string failed", e);
      return null;
    }
  }

  public static String expandEnvVars(String text) {
    Map<String, String> envMap = System.getenv();
    for (Map.Entry<String, String> entry : envMap.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      text = text.replace("%" + key + "%", value);
    }
    return text;
  }

  public static Bundle generateBundle(TenantService tenantService, Report report, EventService eventService, ApiConfig config) {
    FhirBundler bundler = new FhirBundler(eventService, tenantService, config);
    logger.info("Building Bundle for MeasureReport to send...");
    List<Aggregate> aggregates = tenantService.getAggregates(report.getId());
    Bundle bundle = bundler.generateBundle(aggregates, report);
    logger.info(String.format("Done building Bundle for MeasureReport with %s entries", bundle.getEntry().size()));
    return bundle;
  }
}
