package com.lantanagroup.link.db.model.tenant;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Events {
  List<String> BeforeMeasureResolution;

  List<String> AfterMeasureResolution;

  List<String> OnRegeneration;

  List<String> BeforePatientOfInterestLookup;

  List<String> AfterPatientOfInterestLookup;

  List<String> BeforePatientDataQuery;

  List<String> AfterPatientResourceQuery;

  List<String> AfterPatientDataQuery;

  List<String> AfterApplyConceptMaps;

  List<String> BeforePatientDataStore;

  List<String> AfterPatientDataStore;

  List<String> BeforeMeasureEval;

  List<String> AfterMeasureEval;

  List<String> BeforeReportStore;

  List<String> AfterReportStore;

  List<String> BeforeBundling;

  List<String> AfterBundling;
}
