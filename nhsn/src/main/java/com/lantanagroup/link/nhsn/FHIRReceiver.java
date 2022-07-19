package com.lantanagroup.link.nhsn;

import ca.uhn.fhir.rest.client.apache.GZipContentInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.auth.OAuth2Helper;
import com.lantanagroup.link.config.sender.FHIRSenderConfig;
import com.lantanagroup.link.config.sender.FHIRSenderOAuthConfig;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
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
    String fhirServerBase = urlParts.length > 2?url.substring(0, url.indexOf(urlParts[urlParts.length - 2])):url;
    IGenericClient client = FhirContextProvider.getFhirContext().newRestfulGenericClient(fhirServerBase);
    client.registerInterceptor(new GZipContentInterceptor());

    if (StringUtils.isNotEmpty(token)) {
      BearerTokenAuthInterceptor authInterceptor = new BearerTokenAuthInterceptor(token);
      client.registerInterceptor(authInterceptor);
    }

    FhirDataProvider fhirDataProvider = new FhirDataProvider(client);
    return (Bundle) fhirDataProvider.retrieveFromServer(urlParts[urlParts.length - 2], urlParts[urlParts.length - 1]);
  }
}
