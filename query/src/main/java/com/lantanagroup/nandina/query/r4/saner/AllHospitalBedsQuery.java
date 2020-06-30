package com.lantanagroup.nandina.query.r4.saner;

import org.hl7.fhir.r4.model.Resource;

import java.util.Map;

public class AllHospitalBedsQuery extends AbstractSanerQuery {
  @Override
  public Integer execute() {
    Map<String, Resource> data = this.getData();
    return this.countForPopulation(data, "Beds", "numTotBeds");
  }
}
