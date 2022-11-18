package com.lantanagroup.link.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

@Component
public class SamsTokenValidator implements  ITokenValidator {
  private static final Logger logger = LoggerFactory.getLogger(SamsTokenValidator.class);

  @Override
  public boolean verifyToken(String authHeader, String algorithm, String issuer, String jwksUrl, String validationEndpoint) {
    HttpPost request = new HttpPost(validationEndpoint);
    CloseableHttpClient httpClient = HttpClientBuilder.create().build();

    //get token from auth header
    String token = authHeader.substring("Bearer ".length());

    try {
      request.setEntity(new StringEntity(token));
    } catch (UnsupportedEncodingException e) {
      logger.error("Failed to encode token during verification request.");
      return false;
    }

    /*
      Per SAMS documentation:
      Clients can call this service to validate an oAuth Token. The service will return following output:
        If oAuth Token id valid:
        {
        "status":"ok",
        "Reason":"Valid Token"
        }
        If oAuth Token id not valid:
        {
        "status":"fail",
        "Reason":"<Error Message>"
        }
     */

    try (httpClient) {
      logger.info("Requesting token verification from SAMS");
      HttpResponse result = httpClient.execute(request);
      String content = EntityUtils.toString(result.getEntity(), "UTF-8");
      logger.info("SAMS verification response: " + content);

      if (result.getStatusLine() == null || result.getStatusLine().getStatusCode() != 200) {
        logger.error("Error requesting token validation, failed server response.");
      }

      ObjectMapper mapper = new ObjectMapper();
      SamsTokenResult tokenResult = mapper.readValue(content, SamsTokenResult.class);
      return tokenResult.status == "ok";

    } catch (IOException e) {
      logger.error("Error requesting token validation: " + e.getMessage());
      return false;
    }
  }
}

