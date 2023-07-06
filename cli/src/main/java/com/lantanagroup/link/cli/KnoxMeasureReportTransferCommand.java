package com.lantanagroup.link.cli;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.IdentifierHelper;
import com.lantanagroup.link.auth.OAuth2Helper;
import com.lantanagroup.link.config.sender.FHIRSenderConfig;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.MeasureReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@ShellComponent
public class KnoxMeasureReportTransferCommand extends BaseShellCommand {
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("M/d/y");
  private static final String PROFILE_URL =
          "https://hl7.org/fhir/uv/saner/StructureDefinition/PublicHealthMeasureReport";

  private static final Logger logger = LoggerFactory.getLogger(KnoxMeasureReportTransferCommand.class);

  private KnoxMeasureReportTransferConfig config;

  @ShellMethod(
          key = "knox-measure-report-transfer",
          value = "Download CSV data, convert it to one or more measure reports, and submit them to Link.")
  public void execute() throws Exception {
    registerBeans();
    config = applicationContext.getBean(KnoxMeasureReportTransferConfig.class);
    validate(config);
    byte[] data = download();
    for (MeasureReport report : convert(data)) {
      submit(report);
    }
  }

  private byte[] download() throws Exception {
    logger.info("Downloading from {}/{}", config.getDownloader().getHost(), config.getDownloader().getPath());
    SftpDownloader downloader = new SftpDownloader(config.getDownloader());
    return downloader.download();
  }

  private Collection<MeasureReport> convert(byte[] data) throws Exception {
    logger.info("Converting to measure reports");
    try (InputStream stream = new ByteArrayInputStream(data);
         Reader reader = new InputStreamReader(stream)) {
      CSVFormat format = CSVFormat.Builder.create()
              .setHeader()
              .setSkipHeaderRecord(true)
              .build();
      try (CSVParser parser = format.parse(reader)) {
        Map<Date, MeasureReport> reportsByDate = new LinkedHashMap<>();
        for (CSVRecord record : parser.getRecords()) {
          Date date = DATE_FORMAT.parse(record.get("Date"));
          String cvxCode = record.get("CVX Code");
          int quantity = Integer.parseInt(record.get("Quantity"));
          MeasureReport report = reportsByDate.get(date);
          if (report == null) {
            report = getReport(date);
            reportsByDate.put(date, report);
          }
          MeasureReport.MeasureReportGroupPopulationComponent population = report.getGroupFirstRep().addPopulation();
          population.getCode().getCodingFirstRep().setCode(cvxCode);
          population.setCount(quantity);
        }
        return reportsByDate.values();
      }
    }
  }

  private MeasureReport getReport(Date date) {
    MeasureReport report = new MeasureReport();
    report.getMeta().addProfile(PROFILE_URL);
    report.setStatus(MeasureReport.MeasureReportStatus.COMPLETE);
    report.setType(MeasureReport.MeasureReportType.SUMMARY);
    report.setMeasure(config.getMeasureUrl());
    report.getSubject().setIdentifier(IdentifierHelper.fromString(config.getSubjectIdentifier()));
    report.setDate(new Date());
    // DAY precision doesn't conform to PublicHealthMeasureReport, but it matches SanerCSVConverter's output
    report.getPeriod()
            .setStart(date, TemporalPrecisionEnum.DAY)
            .setEnd(date, TemporalPrecisionEnum.DAY);
    report.getGroupFirstRep().getCode().getCodingFirstRep().setCode(config.getGroupCode());
    return report;
  }

  private void submit(MeasureReport report) throws Exception {
    FHIRSenderConfig senderConfig = config.getFhirSender();
    logger.info(
            "Submitting report for {} to {}",
            DATE_FORMAT.format(report.getPeriod().getStart()),
            senderConfig.getUrl());
    FhirDataProvider provider = new FhirDataProvider(senderConfig.getUrl());
    String token = OAuth2Helper.getToken(senderConfig.getAuthConfig());
    if (StringUtils.isNotEmpty(token)) {
      provider.getClient().registerInterceptor(new BearerTokenAuthInterceptor(token));
    }
    provider.createResource(report);
  }
}
