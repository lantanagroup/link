package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.auth.OAuth2Helper;
import com.lantanagroup.link.config.sender.FHIRSenderConfig;
import com.lantanagroup.link.config.sender.FHIRSenderOAuthConfig;
import lombok.Setter;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FHIRReceiver {
  protected static final Logger logger = LoggerFactory.getLogger(FHIRReceiver.class);

  @Autowired
  @Setter
  private FHIRSenderConfig config;

  public CloseableHttpClient getHttpClient() {
    return HttpClientBuilder.create().build();
  }

  public Bundle retrieveContent(String url) throws Exception {
    FHIRSenderOAuthConfig oAuthConfig = this.config.getAuthConfig();
    String token = OAuth2Helper.getToken(oAuthConfig, getHttpClient());

    String[] urlParts = url.split("/");
    FhirDataProvider fhirDataProvider = new FhirDataProvider(urlParts.length > 2?url.substring(0, url.indexOf(urlParts[urlParts.length - 2])):url);
    return (Bundle) fhirDataProvider.retrieveFromServer(token, urlParts[urlParts.length - 2], urlParts[urlParts.length - 1]);
  }
}
