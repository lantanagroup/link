package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.auth.OAuth2Helper;
import com.lantanagroup.link.config.sender.FHIRSenderConfig;
import com.lantanagroup.link.config.sender.FHIRSenderOAuthConfig;
import com.lantanagroup.link.config.sender.FhirSenderUrlOAuthConfig;
import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class FHIRReceiver {
  protected static final Logger logger = LoggerFactory.getLogger(FHIRReceiver.class);

  @Autowired
  @Setter
  private FHIRSenderConfig config;

  public HttpClient getHttpClient() {
    return HttpClientBuilder.create().build();
  }

  public String retrieveContent(String url) throws Exception {

    String content = "";

    Optional<FhirSenderUrlOAuthConfig> foundOauth = this.config.getSendUrls().stream().filter(authConfig -> url.contains(authConfig.getUrl())).findFirst();

    if (foundOauth.isPresent()) {
      FHIRSenderOAuthConfig oAuthConfig = foundOauth.get().getAuthConfig();
      String token = OAuth2Helper.getToken(oAuthConfig, getHttpClient());
      HttpGet getRequest = new HttpGet(url);
      getRequest.addHeader("Content-Type", "application/json");
      if (Strings.isNotEmpty(token)) {
        logger.debug("Adding auth token to submit request");
        getRequest.addHeader("Authorization", "Bearer " + token);
      }
      try {
        HttpClient httpClient = this.getHttpClient();
        HttpResponse response = httpClient.execute(getRequest);
        if (response.getStatusLine().getStatusCode() >= 300) {
          String responseContent = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
          logger.error(String.format("Error (%s) getting report to %s: %s", response.getStatusLine().getStatusCode(), url, responseContent));
          throw new HttpResponseException(500, "Internal Server Error");
        }
        content = new BufferedReader(new InputStreamReader(response.getEntity().getContent())).lines().collect(Collectors.joining("\n"));
      } catch (IOException ex) {
        if (ex.getMessage().contains("403")) {
          logger.error("Error authorizing receive: " + ex.getMessage());
        } else {
          logger.error("Error while receiving MeasureReport bundle from URL", ex);
        }
        throw ex;
      }
    }
    return content;
  }

}
