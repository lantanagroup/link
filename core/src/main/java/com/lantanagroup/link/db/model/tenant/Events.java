package com.lantanagroup.link.db.model.tenant;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * NOTE: Additions/changes to this class should be modified in FhirBundler and TenantController
 */
@Getter
@Setter
public class Events {
  List<String> BeforeMeasureResolution = new ArrayList<>();
  
  List<String> AfterMeasureResolution = new ArrayList<>();

  List<String> OnRegeneration = new ArrayList<>();

  List<String> BeforePatientOfInterestLookup = new ArrayList<>();

  List<String> AfterPatientOfInterestLookup = new ArrayList<>();

  List<String> BeforePatientDataQuery = new ArrayList<>();

  List<String> AfterPatientResourceQuery = new ArrayList<>();

  List<String> AfterPatientDataQuery = new ArrayList<>();

  List<String> AfterApplyConceptMaps = new ArrayList<>();

  List<String> BeforePatientDataStore = new ArrayList<>();

  List<String> AfterPatientDataStore = new ArrayList<>();

  List<String> BeforeMeasureEval = new ArrayList<>();

  List<String> AfterMeasureEval = new ArrayList<>();

  List<String> BeforeReportStore = new ArrayList<>();

  List<String> AfterReportStore = new ArrayList<>();

  List<String> BeforeBundling = new ArrayList<>();

  List<String> AfterBundling = new ArrayList<>();
}
