package com.lantanagroup.link.nhsn;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.EventService;
import com.lantanagroup.link.FhirBundler;
import com.lantanagroup.link.Helper;
import com.lantanagroup.link.IReportDownloader;
import com.lantanagroup.link.config.bundler.BundlerConfig;
import com.lantanagroup.link.db.MongoService;
import com.lantanagroup.link.db.model.Report;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
public class MeasureReportDownloader implements IReportDownloader {
  protected static final Logger logger = LoggerFactory.getLogger(MeasureReportDownloader.class);

  @Autowired
  private MongoService mongoService;

  @Override
  public void download(String reportId, String downloadType, HttpServletResponse response, FhirContext ctx, BundlerConfig config, EventService eventService) throws IOException {
    Report report = this.mongoService.findReport(reportId);

    if (report == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found");
    }

    logger.info("Building Bundle for MeasureReport...");
    FhirBundler bundler = new FhirBundler(config, this.mongoService, eventService);
    Bundle bundle = bundler.generateBundle(report.getAggregates(), report);

    logger.info("Bundle created for MeasureReport including " + bundle.getEntry().size() + " entries");
    String responseBody = "";
    if (downloadType.equals("XML")) {
      responseBody = ctx.newXmlParser().encodeResourceToString(bundle);
      response.setContentType("application/xml");
    } else if (downloadType.equals("JSON")) {
      responseBody = ctx.newJsonParser().encodeResourceToString(bundle);
      response.setContentType("application/json");
    }

    if (Helper.validateHeaderValue(reportId)) {
      response.setHeader("Content-Disposition", "attachment; filename=\"" + reportId + ".xml\"");
    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid report Id");
    }

    InputStream is = new ByteArrayInputStream(responseBody.getBytes());
    IOUtils.copy(is, response.getOutputStream());
    response.flushBuffer();
  }
}
