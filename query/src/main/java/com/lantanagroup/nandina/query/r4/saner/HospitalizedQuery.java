package com.lantanagroup.nandina.query.r4.saner;

import org.hl7.fhir.r4.model.Resource;

import java.util.Map;

public class HospitalizedQuery extends AbstractSanerQuery {
  @Override
  public Integer execute() {
    if (!this.criteria.containsKey("reportDate")) return null;
    Map<String, Resource> data = this.getData();
    this.addContextData("hospitalized", this);
    return this.countForPopulation(data, "Encounters", "numC19HospPats");
  }
}
