package com.lantanagroup.link.consumer.controller;

import ca.uhn.fhir.context.FhirContext;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.lantanagroup.link.FhirContextProvider;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.util.Strings;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@RestController

public class CsvController {

  private static final Logger logger = LoggerFactory.getLogger(CsvController.class);

  public HttpClient getHttpClient() {
    return HttpClientBuilder.create().build();
  }


  /**
   * Receives a CVS representing a Bundle with a MeasureReport
   *
   * @param csvContent The content of the CSV
   */
  @PostMapping(value = "/csv", consumes = "text/csv")
  public void storeCSV(
          Authentication authentication,
          HttpServletRequest request,
          @AuthenticationPrincipal DecodedJWT user,
          @RequestBody() String csvContent) throws Exception {

    logger.info("User subject is: " + user.getSubject());

    String path = new UrlPathHelper().getPathWithinApplication(request);

    logger.info("Request url: " + request.getRequestURL().toString());
    String requestUrl = request.getRequestURL().toString();
    String fhirUrl = requestUrl.substring(0, requestUrl.indexOf(request.getServletPath())) + "/fhir";
    String sendUrl = fhirUrl + "/Bundle";

    //TODO: Replace with CSV->Bundle conversion logic
    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.COLLECTION);
    String content = FhirContextProvider.getFhirContext().newXmlParser().encodeResourceToString(bundle);
    sendRequest(user, sendUrl, content);

    // IGenericClient client = FhirContextProvider.getFhirContext().newRestfulGenericClient(fhirUrl);
//    MethodOutcome resourceCreated = client.create().resource(content).execute();
//    logger.info("Bundle created: " + resourceCreated.getId());

  }

  private void sendRequest(DecodedJWT user, String url, String content) throws UnsupportedEncodingException {
    HttpPost sendRequest = new HttpPost(url);
    sendRequest.addHeader("Content-Type", "application/xml");

    if (Strings.isNotEmpty(user.getToken())) {
      logger.debug("Adding auth token to submit request");
      sendRequest.addHeader("Authorization", "Bearer " + user.getToken());
    }

    sendRequest.setEntity(new StringEntity(content));

    try {
      HttpClient httpClient = getHttpClient();

      HttpResponse response = httpClient.execute(sendRequest);

      if (response.getStatusLine().getStatusCode() >= 300) {
        String responseContent = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        logger.error(String.format("Error (%s) submitting request to %s: %s", response.getStatusLine().getStatusCode(), url, responseContent));
        throw new HttpResponseException(500, "Internal Server Error");
      } else {
        logger.info("Response is: " + response.getStatusLine());
      }
    } catch (Exception ex) {
      logger.error(String.format("Error (%s) submitting request", ex.getMessage()));
    }
  }

}
