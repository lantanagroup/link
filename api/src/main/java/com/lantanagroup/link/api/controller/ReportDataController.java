package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.instance.model.api.IIdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.http.client.HttpResponseException;
import org.hl7.fhir.r4.model.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;


@RestController
@RequestMapping("/api/fhir")
public class ReportDataController extends BaseController{
  private static final Logger logger = LoggerFactory.getLogger(ReportDataController.class);

  @PostMapping(value = "/{resourceType}")
  public String storeReportData(Authentication authentication,
                              HttpServletRequest request,
                              @PathVariable String resourceType,
                              @RequestBody() Resource resource) throws Exception{

    if(resource.getResourceType() != null && !resource.getResourceType().toString().equals(resourceType)){
      throw new HttpResponseException(500, "Resource type in path and submitted resource's type must match");
    }

    IGenericClient fhirStoreClient = this.getFhirStoreClient(authentication, request);
    Bundle searchResults = fhirStoreClient.search()
            .forResource(resourceType)
            .where(Resource.RES_ID.exactly().identifier(resource.getId()))
            .returnBundle(Bundle.class)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();

    if(searchResults.hasEntry()){
      throw new HttpResponseException(500, "Resource with id " + resource.getId() + " already exists");
    }
    else{
      MethodOutcome outcome = this.createResource(resource, fhirStoreClient);
      return "Stored FHIR resource with new ID of " + outcome.getResource().getIdElement().getIdPart();
    }
  }

  @PutMapping(value = "/{resourceType}/{resourceId}")
  public String updateReportData(Authentication authentication,
                               HttpServletRequest request,
                               @PathVariable("resourceType") String resourceType,
                               @PathVariable("resourceId") String resourceId,
                               @RequestBody() Resource resource) throws Exception{

    if(resource.getResourceType() != null && !resource.getResourceType().toString().equals(resourceType)){
      throw new HttpResponseException(500, "Resource type in path and submitted resource's type must match");
    }

    IGenericClient fhirStoreClient = this.getFhirStoreClient(authentication, request);
    Bundle searchResults = fhirStoreClient.search()
            .forResource(resourceType)
            .where(Resource.RES_ID.exactly().identifier(resourceId))
            .returnBundle(Bundle.class)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();

    if(searchResults.hasEntry()){
      MethodOutcome outcome = this.updateResource(resource, fhirStoreClient);

      return String.format("Update is successful for %s/%s", ((DomainResource)outcome.getResource()).getResourceType().toString(), ((DomainResource)outcome.getResource()).getIdElement().getIdPart());
    }
    else{
      throw new HttpResponseException(500, "Resource with resourceID " + resourceId + " does not exist");
    }
  }

  @Override
  public MethodOutcome createResource(Resource resource, IGenericClient fhirStoreClient){
    MethodOutcome outcome = fhirStoreClient.update().resource(resource).execute();
    if (!outcome.getCreated() || outcome.getResource() == null) {
      logger.error("Failed to store/create FHIR resource");
    } else {
      logger.debug("Stored FHIR resource with new ID of " + outcome.getResource().getIdElement().getIdPart());
    }
    return outcome;
  }
}
