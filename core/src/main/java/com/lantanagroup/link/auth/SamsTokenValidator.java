package com.lantanagroup.link.auth;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class SamsTokenValidator implements  ITokenValidator {
  private static final Logger logger = LoggerFactory.getLogger(SamsTokenValidator.class);
  String tokenValidationUrl = ""; //get from config

  @Override
  public boolean verifyToken(String authHeader, String algorithm, String issuer, String jwksUrl) {
    HttpPost request = new HttpPost(tokenValidationUrl);
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
      HttpResponse result = httpClient.execute(request);
      return result.getStatusLine().getStatusCode() == 200;

    } catch (IOException e) {
      logger.error("Error requesting token validation: " + e.getMessage());
      return false;
    }
  }
}
