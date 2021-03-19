package com.lantanagroup.nandina.query.api.controller;

import com.lantanagroup.nandina.config.IQueryConfig;
import com.lantanagroup.nandina.query.Query;
import com.lantanagroup.nandina.query.api.config.QueryConfig;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QueryController {
  private static final Logger logger = LoggerFactory.getLogger(QueryController.class);

  @Autowired
  private QueryConfig config;

  @GetMapping(value = "/api/data", produces = {"application/json", "application/fhir+json", "application/xml", "application/fhir+xml"})
  public @ResponseBody Bundle getData(String[] patientIdentifier) {
    Query query = new Query(this.config);
    return query.execute(patientIdentifier);
  }
}
