package com.lantanagroup.nandina.query.pihc.fhir.r4.saner;

public abstract class Constants {
  public static final String MEASURE_GROUP_SYSTEM = "http://hl7.org/fhir/us/saner/CodeSystem/MeasureGroupSystem";
  public static final String[] MEASURE_POPULATION_SYSTEMS = new String[] {
          "http://hl7.org/fhir/us/saner/CodeSystem/MeasuredValues",
          "http://hl7.org/fhir/us/saner/CodeSystem/MeasurePopulationSystem",
          "http://hl7.org/fhir/us/saner/CodeSystem/MeasurePopulationSystem"
  };
  public static final String MEASURE_URL = "http://hl7.org/fhir/us/saner/Measure/CDCPatientImpactAndHospitalCapacity";
}
