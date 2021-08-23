package com.lantanagroup.link.nhsn;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.IReportSender;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.sender.LeidosSenderConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MeasureReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
public class LeidosSender implements IReportSender {
  protected static final Logger logger = LoggerFactory.getLogger(LeidosSender.class);

  @Autowired
  private LeidosSenderConfig config;

  @Override
  public void send (MeasureReport report, FhirContext ctx, HttpServletRequest request, Authentication auth, IGenericClient fhirStoreClient) throws Exception {
    if (this.config.getSendUrls() == null || this.config.getSendUrls().isEmpty()) {
      throw new Exception("Not configured with any locations to send");
    }

    logger.info("Building Bundle for MeasureReport to send...");

    Bundle bundle = FhirHelper.bundleMeasureReport(report, fhirStoreClient);

    logger.info("Bundle created for MeasureReport including " + bundle.getEntry().size() + " entries");

    String xml = ctx.newXmlParser().encodeResourceToString(bundle);

    logger.trace(String.format("Configured to send to %s locations", this.config.getSendUrls().size()));

    for (String sendUrl : this.config.getSendUrls()) {
      logger.info("Sending MeasureReport bundle to URL " + sendUrl);

      HttpPost sendRequest = new HttpPost(sendUrl);
      sendRequest.addHeader("Content-Type", "application/xml");
      sendRequest.setEntity(new StringEntity(xml));

      try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
        // HttpResponse result = httpClient.execute(request);
        // String response = EntityUtils.toString(result.getEntity(), "UTF-8");
        httpClient.execute(sendRequest);

        FhirHelper.recordAuditEvent(request, fhirStoreClient, ((LinkCredentials) auth.getPrincipal()).getJwt(), FhirHelper.AuditEventTypes.Send, String.format("Successfully sent report to %s", sendUrl));
      } catch (IOException ex) {
        logger.error("Error while sending MeasureReport bundle to URL", ex);
        throw ex;
      }
    }
  }
}
