package com.lantanagroup.link;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.apache.GZipContentInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import com.lantanagroup.link.auth.OAuth2Helper;
import com.lantanagroup.link.config.bundler.BundlerConfig;
import com.lantanagroup.link.config.sender.FHIRSenderConfig;
import com.lantanagroup.link.db.MongoService;
import com.lantanagroup.link.db.model.Report;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public abstract class GenericSender {
  protected static final Logger logger = LoggerFactory.getLogger(GenericSender.class);

  // TODO: This should be re-factored out of this GenericSender class
  @Autowired
  @Setter
  private FHIRSenderConfig fhirSenderConfig;

  @Autowired
  private EventService eventService;

  @Autowired
  private MongoService mongoService;

  @Autowired
  private BundlerConfig bundlerConfig;

  public Bundle generateBundle(Report report) {
    logger.info("Building Bundle for MeasureReport to send...");
    FhirBundler bundler = new FhirBundler(this.bundlerConfig, this.mongoService, this.eventService);
    Bundle bundle = bundler.generateBundle(report.getAggregates(), report);
    logger.info(String.format("Done building Bundle for MeasureReport with %s entries", bundle.getEntry().size()));
    return bundle;
  }

  public CloseableHttpClient getHttpClient() {
    return HttpClientBuilder.create().build();
  }

  public abstract String bundle(Bundle bundle, FhirDataProvider fhirProvider, String type);

  public String sendContent(Resource resourceToSend, Report report) throws Exception {
    if (StringUtils.isEmpty(this.fhirSenderConfig.getUrl())) {
      throw new IllegalStateException("Not configured with any locations to send");
    }

    Resource copy = resourceToSend.copy();
    IGenericClient client = FhirContextProvider.getFhirContext().newRestfulGenericClient(this.fhirSenderConfig.getUrl());
    client.registerInterceptor(new GZipContentInterceptor());

    String token = OAuth2Helper.getToken(this.fhirSenderConfig.getAuthConfig(), getHttpClient());

    if (StringUtils.isNotEmpty(token)) {
      BearerTokenAuthInterceptor authInterceptor = new BearerTokenAuthInterceptor(token);
      client.registerInterceptor(authInterceptor);
    }

    FhirDataProvider consumerFhirProvider = new FhirDataProvider(client);

    logger.info("Sending MeasureReport bundle to URL " + this.fhirSenderConfig.getUrl());

    MethodOutcome outcome;

    if (copy.hasId()) {
      outcome = consumerFhirProvider.updateResource(copy);
    } else {
      outcome = consumerFhirProvider.createResource(copy);
    }

    List<String> locations = outcome.getResponseHeaders().get("content-location");

    if (locations != null && locations.size() == 1) {
      return locations.get(0);
    }

    return null;
  }
}
