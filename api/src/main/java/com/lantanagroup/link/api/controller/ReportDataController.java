package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.csv.CSVEntry;
import com.lantanagroup.link.model.CsvEntry;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.http.client.HttpResponseException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;


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

    Bundle searchResults = this.getFhirDataProvider().getResources(Resource.RES_ID.exactly().identifier(resource.getId()), resourceType);

    if(searchResults.hasEntry()){
      throw new HttpResponseException(500, "Resource with id " + resource.getId() + " already exists");
    }
    else {
      Resource resourceCreated = this.getFhirDataProvider().createResource(resource);
      return "Stored FHIR resource with new ID of " + resourceCreated.getIdElement().getIdPart();
    }
  }

  @PutMapping(value = "/{resourceType}/{resourceId}")
  public String updateReportData(Authentication authentication,
                               HttpServletRequest request,
                               @PathVariable("resourceType") String resourceType,
                               @PathVariable("resourceId") String resourceId,
                               @RequestBody() Resource resource) throws Exception {

    if (resource.getResourceType() != null && !resource.getResourceType().toString().equals(resourceType)) {
      throw new HttpResponseException(500, "Resource type in path and submitted resource's type must match");
    }

    Bundle searchResults = this.getFhirDataProvider().getResources(Resource.RES_ID.exactly().identifier(resource.getId()), resourceType);


    if (searchResults.hasEntry()) {
      this.getFhirDataProvider().updateResource(resource);
      return String.format("Update is successful for %s/%s", resource.getResourceType().toString(), resource.getIdElement().getIdPart());
    } else {
      throw new HttpResponseException(500, "Resource with resourceID " + resourceId + " does not exist");
    }
  }

  @PostMapping(value = "/api/data/csv?type=XXX")
  public void retrieveCSVData(@PathVariable("XXX") String type, @RequestBody() String csvContent) throws Exception {

    logger.debug("Receiving CSV. Parsing...");
    InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
    CSVReader csvReader = new CSVReaderBuilder(bufferedReader).withSkipLines(1).build();
    List<String[]> csvData = csvReader.readAll();
    switch (type) {
      case "bed":
        // TODO
        break;
      case "ventilator":
        // TODO
        break;
    }
  }

}