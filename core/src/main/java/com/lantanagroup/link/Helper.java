package com.lantanagroup.link;

import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.Aggregate;
import com.lantanagroup.link.db.model.Report;
import com.lantanagroup.link.model.ApiVersionModel;
import com.microsoft.sqlserver.jdbc.SQLServerDriver;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
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

  public static ApiVersionModel getVersionInfo() {
    try {
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

      URL buildFile = Helper.class.getClassLoader().getResource("build.yml");

      if (buildFile == null) {
        logger.warn("No build.yml file found, returning default \"dev\" build and \"0.9.0\" version");
        return new ApiVersionModel();
      }

      return mapper.readValue(buildFile, ApiVersionModel.class);
    } catch (IOException ex) {
      logger.error("Error deserializing build.yml file", ex);
      return new ApiVersionModel();
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

  public static Date parseFhirDate(String dateStr) {
    return new DateTimeType(dateStr).getValue();
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

  public static void dumpToFile(Resource resource, String path, String fileName) {

    IParser parser = FhirContextProvider.getFhirContext().newJsonParser();
    String folderPath = Helper.expandEnvVars(path);
    String filePath = Paths.get(folderPath, fileName).toString();
    try (Writer writer = new FileWriter(filePath, StandardCharsets.UTF_8)) {
      parser.encodeResourceToWriter(resource, writer);
    }
    catch (Exception e) {
      logger.error("Error writing resource {} to file system", resource.getId(), e);
    }
    finally {
      logger.info("Done writing resource {} to file system {}", resource.getId(), filePath);
    }
  }
}
