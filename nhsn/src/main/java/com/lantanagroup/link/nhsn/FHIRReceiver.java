package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.FhirDataProvider;
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
import org.hl7.fhir.r4.model.Bundle;
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

  public Bundle retrieveContent(String url) throws Exception {

    Optional<FhirSenderUrlOAuthConfig> foundOauth = this.config.getSendUrls().stream().filter(authConfig -> url.contains(authConfig.getUrl())).findFirst();
    Bundle bundle = null;
    if (foundOauth.isPresent()) {
      FHIRSenderOAuthConfig oAuthConfig = foundOauth.get().getAuthConfig();
      String token = OAuth2Helper.getToken(oAuthConfig, getHttpClient());

      String[] urlParts = url.split("/");
      FhirDataProvider fhirDataProvider = new FhirDataProvider(urlParts.length > 2?url.substring(0, url.indexOf(urlParts[urlParts.length - 2])):url);
      bundle = fhirDataProvider.retrieveFromServer(token, urlParts[urlParts.length - 2], urlParts[urlParts.length - 1]);
    }
    return bundle;
  }
}
