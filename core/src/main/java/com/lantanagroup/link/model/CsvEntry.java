package com.lantanagroup.link.model;

import com.lantanagroup.link.Helper;
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

  public CsvEntry(String patientIdentifier, String start, String end, String patientLogicalID) {
    this.patientIdentifier = patientIdentifier;
    this.periodIdentifier = start + ":" + end;
    this.period = new Period();
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    try {
      Date startDate = format.parse(start);
      Date endDate = format.parse(end);
      this.period.setStart(Helper.getStartOfDay(startDate));
      this.period.setEnd(Helper.getEndOfDay(endDate, 0));
    } catch (ParseException e) {
      e.printStackTrace();
    }
    this.patientLogicalID = patientLogicalID;
  }
}
