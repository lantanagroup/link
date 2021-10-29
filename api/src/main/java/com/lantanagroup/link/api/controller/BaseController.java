package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.FhirDataProvider;
import com.lantanagroup.link.config.api.ApiConfig;
import lombok.Setter;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;

public class BaseController {
  private static final Logger logger = LoggerFactory.getLogger(BaseController.class);
  protected FhirContext ctx = FhirContext.forR4();
  @Autowired
  private ApiConfig config;

  @Setter
  private IGenericClient fhirStoreClient;

  protected IGenericClient getFhirStoreClient() {
    if (this.fhirStoreClient == null) {
      String fhirBase = config.getFhirServerStore();
      IGenericClient fhirStoreClient = this.ctx.newRestfulGenericClient(fhirBase);
      this.fhirStoreClient = fhirStoreClient;
    }

    return this.fhirStoreClient;
  }

  @Setter
  private FhirDataProvider fhirStoreProvider;

  protected FhirDataProvider getFhirStoreProvider() {
    if (this.fhirStoreProvider == null) {
      this.fhirStoreProvider = new FhirDataProvider(this.getFhirStoreClient());
    }
    return this.fhirStoreProvider;
  }

  public BaseController() {
    this.ctx.getRestfulClientFactory().setSocketTimeout(200 * 5000);
  }

  protected void createResource(Resource resource, IGenericClient fhirStoreClient) {
    MethodOutcome outcome = fhirStoreClient.create().resource(resource).execute();
    if (!outcome.getCreated() || outcome.getResource() == null) {
      logger.error("Failed to store/create FHIR resource");
    } else {
      logger.debug("Stored FHIR resource with new ID of " + outcome.getResource().getIdElement().getIdPart());
    }
  }

  protected void updateResource(Resource resource, IGenericClient fhirStoreClient) {
    int initialVersion = resource.getMeta().getVersionId() != null?Integer.parseInt(resource.getMeta().getVersionId()):0;

    // Make sure the ID is not version-specific
    if (resource.getIdElement() != null && resource.getIdElement().getIdPart() != null) {
      resource.setId(resource.getIdElement().getIdPart());
    }

    MethodOutcome outcome = fhirStoreClient.update().resource(resource).execute();
    DomainResource domainResource = (DomainResource) outcome.getResource();
    int updatedVersion = Integer.parseInt(outcome.getId().getVersionIdPart());
    if (updatedVersion > initialVersion) {
      logger.debug(String.format("Update is successful for %s/%s", domainResource.getResourceType().toString(), domainResource.getIdElement().getIdPart()));
    } else {
      logger.error(String.format("Failed to update FHIR resource %s/%s", domainResource.getResourceType().toString(), domainResource.getIdElement().getIdPart()));
    }
  }
}
