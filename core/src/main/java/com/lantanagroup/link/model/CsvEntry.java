package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CsvEntry {
  String patientIdentifier;
  String date;
  String encounterID;
  String patientLogicalID;

  public CsvEntry(String patientIdentifier, String date, String encounterID, String patientLogicalID) {
    this.patientIdentifier = patientIdentifier;
    this.date = date;
    this.encounterID = encounterID;
    this.patientLogicalID = patientLogicalID;
  }
}
