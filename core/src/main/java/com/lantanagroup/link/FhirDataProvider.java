package com.lantanagroup.link;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.apache.GZipContentInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import com.lantanagroup.link.config.api.ApiDataStoreConfig;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FhirDataProvider {
  private static final Logger logger = LoggerFactory.getLogger(FhirDataProvider.class);
  protected FhirContext ctx = FhirContextProvider.getFhirContext();

  @Getter
  private IGenericClient client;

  public FhirDataProvider(IGenericClient client) {
    this.client = client;
  }

  public FhirDataProvider(ApiDataStoreConfig config) {
    IGenericClient client = FhirContextProvider.getFhirContext().newRestfulGenericClient(config.getBaseUrl());
    if (StringUtils.isNotEmpty(config.getUsername()) && StringUtils.isNotEmpty(config.getPassword())) {
      BasicAuthInterceptor authInterceptor = new BasicAuthInterceptor(config.getUsername(), config.getPassword());
      client.registerInterceptor(authInterceptor);
    }
    this.client = client;
  }

  public FhirDataProvider(String fhirBase) {
    this.client = this.ctx.newRestfulGenericClient(fhirBase);
    this.client.registerInterceptor(new GZipContentInterceptor());
  }

  public MethodOutcome updateResource(IBaseResource resource) {
    int initialVersion = resource.getMeta().getVersionId() != null ? Integer.parseInt(resource.getMeta().getVersionId()) : 0;

    // Make sure the ID is not version-specific
    if (resource.getIdElement() != null && resource.getIdElement().getIdPart() != null) {
      resource.setId(resource.getIdElement().getIdPart());
    }

    MethodOutcome outcome = this.client
            .update()
            .resource(resource)
            .execute();

    Resource domainResource = (Resource) outcome.getResource();
    int updatedVersion = Integer.parseInt(outcome.getId().getVersionIdPart());
    if (updatedVersion > initialVersion) {
      logger.debug(String.format("Update is successful for %s/%s", domainResource.getResourceType().toString(), domainResource.getIdElement().getIdPart()));
    } else {
      logger.info(String.format("Nothing changed in resource %s/%s", domainResource.getResourceType().toString(), domainResource.getIdElement().getIdPart()));
    }

    return outcome;
  }

  public Bundle getBundleById(String bundleId) {
    Bundle report = this.client
            .read()
            .resource(Bundle.class)
            .withId(bundleId)
            .execute();

    return report;
  }

  public Bundle transaction(Bundle txBundle) {
    logger.trace("Executing transaction on " + this.client.getServerBase());

    Bundle responseBundle = this.client
            .transaction()
            .withBundle(txBundle)
            .execute();

    return responseBundle;
  }

  public MeasureReport getMeasureReport(String measureId, Parameters parameters) {
    MeasureReport measureReport = client.operation()
            .onInstance(new IdType("Measure", measureId))
            .named("$evaluate-measure")
            .withParameters(parameters)
            .returnResourceType(MeasureReport.class)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();
    return measureReport;
  }

  public MethodOutcome createResource(IBaseResource resource) {
    return this.client
            .create()
            .resource(resource)
            .prettyPrint()
            .encodedJson()
            .execute();
  }
}
