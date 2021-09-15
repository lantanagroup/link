package com.lantanagroup.link.query.api.controller;

import com.lantanagroup.link.config.query.QueryConfig;
import com.lantanagroup.link.model.PatientOfInterestModel;
import com.lantanagroup.link.query.IQuery;
import com.lantanagroup.link.query.QueryFactory;
import org.apache.http.client.HttpResponseException;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class QueryController {
  private static final Logger logger = LoggerFactory.getLogger(QueryController.class);

  @Autowired
  private ApplicationContext context;

  @Autowired
  private QueryConfig config;

  @GetMapping(value = "/api/data", produces = {"application/json", "application/fhir+json", "application/xml", "application/fhir+xml"})
  public @ResponseBody Bundle getData(String[] patientIdentifier, String[] patientRef) throws HttpResponseException {
    IQuery query = null;

    try {
      QueryConfig queryConfig = this.context.getBean(QueryConfig.class);
      query = QueryFactory.getQueryInstance(this.context, queryConfig.getQueryClass());
    } catch (Exception ex) {
      logger.error("Error instantiating instance of IQuery", ex);
      throw new HttpResponseException(500, "Internal Server Error");
    }

    List<PatientOfInterestModel> poiIdentifiers = patientIdentifier != null ? Arrays.stream(patientIdentifier).map(ident -> {
      PatientOfInterestModel poi = new PatientOfInterestModel();
      poi.setIdentifier(ident);
      return poi;
    }).collect(Collectors.toList()) : new ArrayList<>();
    List<PatientOfInterestModel> poiRefs = patientRef != null ? Arrays.stream(patientRef).map(ref -> {
      PatientOfInterestModel poi = new PatientOfInterestModel();
      poi.setReference(ref);
      return poi;
    }).collect(Collectors.toList()) : new ArrayList<>();

    List<PatientOfInterestModel> allPatientsOfInterest = new ArrayList<>();
    allPatientsOfInterest.addAll(poiIdentifiers);
    allPatientsOfInterest.addAll(poiRefs);

    return query.execute(allPatientsOfInterest);
  }
}
