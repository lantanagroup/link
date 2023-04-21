package com.lantanagroup.link.nhsn;

import ca.uhn.fhir.parser.IParser;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.GenericSender;
import com.lantanagroup.link.IReportSender;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.sender.FileSystemSenderConfig;
import com.lantanagroup.link.db.model.Report;
import lombok.Setter;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Component
public class FileSystemSender extends GenericSender implements IReportSender {
  protected static Logger logger = LoggerFactory.getLogger(FileSystemSender.class);

  @Autowired
  @Setter
  private FileSystemSenderConfig config;

  public static String expandEnvVars(String text) {
    Map<String, String> envMap = System.getenv();
    for (Map.Entry<String, String> entry : envMap.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      text = text.replaceAll("%" + key + "%", value);
    }
    return text;
  }

  private FileSystemSenderConfig.Formats getFormat() {
    if (this.config == null || this.config.getFormat() == null) {
      return FileSystemSenderConfig.Formats.JSON;
    }
    return this.config.getFormat();
  }

  public Path getFilePath() {
    String suffix = ".json";

    switch (this.getFormat()) {
      case XML:
        suffix = ".xml";
        break;
    }

    String fileName = "submission-" + (new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(new Date())) + suffix;
    String path;

    if (this.config == null || this.config.getPath() == null || this.config.getPath().length() == 0) {
      logger.info("Not configured with a path to store the submission bundle. Using the system temporary directory");
      path = System.getProperty("java.io.tmpdir");
    } else {
      path = expandEnvVars(this.config.getPath());
    }

    return Paths.get(path, fileName);
  }

  @SuppressWarnings("unused")
  @Override
  public void send(Report report, HttpServletRequest request, LinkCredentials user) throws Exception {
    Bundle bundle = this.generateBundle(report);

    FileSystemSenderConfig.Formats format = this.getFormat();
    String content;
    IParser parser;

    switch (format) {
      case JSON:
        parser = FhirContextProvider.getFhirContext().newJsonParser();
        break;
      case XML:
        parser = FhirContextProvider.getFhirContext().newXmlParser();
        break;
      default:
        throw new Exception(String.format("Unexpected format %s", format));
    }

    if (this.config != null && this.config.getPretty() == true) {
      parser.setPrettyPrint(true);
    }

    logger.info(String.format("Encoding submission bundle to %s", format));
    try (Writer writer = new FileWriter(this.getFilePath().toString(), StandardCharsets.UTF_8)) {
      parser.encodeResourceToWriter(bundle, writer);
    }
    logger.info(String.format("Done encoding submission bundle to %s", format));
  }
}
