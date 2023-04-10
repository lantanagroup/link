package com.lantanagroup.link;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.apache.GZipContentInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import com.lantanagroup.link.auth.OAuth2Helper;
import com.lantanagroup.link.config.sender.FHIRSenderConfig;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.Aggregate;
import com.lantanagroup.link.db.model.Report;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

public abstract class GenericSender {
  protected static final Logger logger = LoggerFactory.getLogger(GenericSender.class);

  // TODO: This should be re-factored out of this GenericSender class
  @Autowired
  @Setter
  private FHIRSenderConfig fhirSenderConfig;

  @Autowired
  private EventService eventService;

  public Bundle generateBundle(TenantService tenantService, Report report) {
    logger.info("Building Bundle for MeasureReport to send...");
    FhirBundler bundler = new FhirBundler(tenantService, this.eventService);
    List<Aggregate> aggregates = tenantService.getAggregates(report.getAggregates());
    List<MeasureReport> aggregateReports = aggregates.stream().map(a -> a.getReport()).collect(Collectors.toList());
    Bundle bundle = bundler.generateBundle(aggregateReports, report);
    logger.info(String.format("Done building Bundle for MeasureReport with %s entries", bundle.getEntry().size()));
    return bundle;
  }

  public CloseableHttpClient getHttpClient() {
    return HttpClientBuilder.create().build();
  }

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
