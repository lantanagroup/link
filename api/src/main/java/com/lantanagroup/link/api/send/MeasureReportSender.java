package com.lantanagroup.link.api.send;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.QueryReport;
import com.lantanagroup.link.api.FhirHelper;
import com.lantanagroup.link.api.config.ApiConfig;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MeasureReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MeasureReportSender implements IReportSender {
  protected static final Logger logger = LoggerFactory.getLogger(MeasureReportSender.class);

  @Override
  public void send(QueryReport report, ApiConfig config, FhirContext ctx) throws Exception {
    String measureReportJson = (String) report.getAnswer("measureReport");
    MeasureReport measureReport = (MeasureReport) ctx.newJsonParser().parseResource(measureReportJson);

    logger.info("Building Bundle for MeasureReport...");

    IGenericClient fhirServerStore = ctx.newRestfulGenericClient(config.getFhirServerStore());
    Bundle bundle = FhirHelper.bundleMeasureReport(measureReport, fhirServerStore, ctx, config.getFhirServerStore());

    logger.info("Bundle created for MeasureReport including " + bundle.getEntry().size() + " entries");

    String xml = ctx.newXmlParser().encodeResourceToString(bundle);

    logger.info("Sending MeasureReport bundle to URL " + config.getSendUrl());

    HttpPost request = new HttpPost(config.getSendUrl());
    request.addHeader("Content-Type", "application/xml");
    request.setEntity(new StringEntity(xml));

    try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
      HttpResponse result = httpClient.execute(request);
      String response = EntityUtils.toString(result.getEntity(), "UTF-8");
    } catch (IOException ex) {
      logger.error("Error while sending MeasureReport bundle to URL", ex);
      throw ex;
    }
  }
}
