package com.lantanagroup.link.measureeval.controller;

import org.hl7.fhir.r4.model.Bundle;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeasureEvalController {
  @PostMapping("/:measureId/$evaluate-measure")
  public String evaluateMeasure(String measureId, @RequestBody Bundle patientData) {
    return "test";
  }
}
