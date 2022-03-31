package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Period;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Getter
@Setter
public class CsvEntry {
  String patientIdentifier;
  Period period;
  String periodIdentifier;
  String encounterID;
  String patientLogicalID;

  public CsvEntry(String patientIdentifier, String start, String end, String encounterID, String patientLogicalID) {
    this.patientIdentifier = patientIdentifier;
    this.periodIdentifier = start + ":" + end;
    this.period = new Period();
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    try {
      Date startDate = format.parse(start);
      Date endDate = format.parse(end);
      this.period.setStart(startDate);
      this.period.setEnd(endDate);
    } catch (ParseException e) {
      e.printStackTrace();
    }
    this.encounterID = encounterID;
    this.patientLogicalID = patientLogicalID;
  }
}
