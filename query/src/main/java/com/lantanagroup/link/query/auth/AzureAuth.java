package com.lantanagroup.link.query.auth;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AzureAuth implements ICustomAuth {
  private static final Logger logger = LoggerFactory.getLogger(AzureAuth.class);

  @Autowired
  private AzureAuthConfig config;

  @Override
  public String getAuthHeader() throws Exception {
    String requestBody = String.format(
            "grant_type=client_credentials&client_id=%s&client_secret=%s&resource=%s",
            this.config.getClientId(),
            this.config.getSecret(),
            this.config.getResource());

    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder(new URI(this.config.getTokenUrl()))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    String responseBody = response.body();
    Object responseObj = new Gson().fromJson(responseBody, Object.class);

    if (responseObj != null) {
      LinkedTreeMap<String, Object> responseTreeMap = (LinkedTreeMap<String, Object>) responseObj;

      if (responseTreeMap.containsKey("access_token")) {
        String accessToken = (String) responseTreeMap.get("access_token");
        logger.debug("Azure access token for queries: " + accessToken);
        return "Bearer " + accessToken;
      } else {
        logger.error("Response from Azure for auth token request does not include an 'access_token' property");
      }
    }
    return null;
  }

  @Override
  public String getApiKeyHeader() throws Exception {
    return null;
  }
}
