package com.lantanagroup.nandina.query.fhir.r4.saner;

import org.hl7.fhir.r4.model.Resource;

import java.util.Map;

public class HospitalizedAndVentilatedQuery extends AbstractSanerQuery {

  @Override
  public Integer execute() {
    Map<String, Resource> data = this.getData();
    return this.countForPopulation(data, "Encounters", "numC19MechVentPats");
  }
}
