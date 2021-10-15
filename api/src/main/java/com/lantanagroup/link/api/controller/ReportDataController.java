package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.lantanagroup.link.serialize.FhirJsonSerializer;
import org.apache.http.client.HttpResponseException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.json.JSONObject;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.Locale;

@RestController
@RequestMapping("/api/fhir")
public class ReportDataController extends BaseController{

  @PostMapping(value = "/{resourceType}",
                consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
  public void storeReportData(Authentication authentication,
                              HttpServletRequest request,
                              @PathVariable String resourceType,
                              @RequestBody() String resource) throws Exception{

    String contentType = request.getContentType();
    Resource parsedResource = parseResource(resource, resourceType, contentType);


    IGenericClient fhirStoreClient = this.getFhirStoreClient(authentication, request);
    Bundle searchResults = fhirStoreClient.search()
            .forResource("Resource")
            .where(Resource.RES_ID.exactly().identifier(parsedResource.getId()))
            .returnBundle(Bundle.class)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();

    if(searchResults.hasEntry()){
      throw new HttpResponseException(500, "Resource with id " + parsedResource.getId() + " already exists");
    }
    else{
      this.createResource(parsedResource, fhirStoreClient);
    }
  }

  @PutMapping(value = "/{resourceType}/{resourceId}")
  public void updateReportData(@RequestParam("resourceType") String resourceType,
                               @RequestParam("resourceId") String resourceId,
                               @RequestBody() Resource resource){

  }

  public Resource parseResource(String resource, String resourceType, String contentType) throws Exception{
    Class clazz = Class.forName(resourceType);
    Object newResource = null;
    if(contentType.toLowerCase().contains("json")){
      Object obj = new JSONObject(resource);
      Constructor constructor = clazz.getConstructor(clazz);
      newResource = constructor.newInstance(obj);
    }
    else if(contentType.toLowerCase().contains("xml")){
      XmlMapper xmlMapper = new XmlMapper();
      newResource = xmlMapper.readValue(resource, clazz);
    }

    return (Resource) newResource;
  }

}
