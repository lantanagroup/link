package com.lantanagroup.nandina.auth;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Base64;

public class OAuth2Helper {
  private static final Logger logger = LoggerFactory.getLogger(OAuth2Helper.class);

  public static String getToken(String tokenUrl, String username, String password, String scope) {
    HttpPost request = new HttpPost(tokenUrl);

    String userPassCombo = username + ":" + password;
    String authorization = Base64.getEncoder().encodeToString(userPassCombo.getBytes());

    request.addHeader("Accept", "application/json");
    request.addHeader("Content-Type", "application/x-www-form-urlencoded");
    request.addHeader("Authorization", "Basic " + authorization);
    request.addHeader("Cache-Control", "no-cache");

    StringBuilder sb = new StringBuilder();
    sb.append("grant_type=client_credentials&");
    sb.append("scope=" + scope);

    try {
      request.setEntity(new StringEntity(sb.toString()));
    } catch (Exception ex) {
      logger.error(ex.getMessage());
      return null;
    }

    try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
      HttpResponse result = httpClient.execute(request);

      String content = EntityUtils.toString(result.getEntity(), "UTF-8");

      if (result.getStatusLine() == null || result.getStatusLine().getStatusCode() != 200) {
        logger.error("Error retrieving OAuth2 token from auth service: \n" + content);
      }

      JSONObject jsonObject = new JSONObject(content);

      if (jsonObject.has("access_token")) {
        return jsonObject.getString("access_token");
      }

      return null;
    } catch (IOException ex) {
      logger.error("Failed to retrieve a token from OAuth2 authorization service", ex);
      return null;
    }
  }
}
