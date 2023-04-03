package com.lantanagroup.link.db.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.hl7.fhir.r4.model.MeasureReport;

@Getter
@Setter
@NoArgsConstructor
public class Aggregate {
  private String id = (new ObjectId()).toString();
  private MeasureReport report;
  public Aggregate(MeasureReport measureReport) {
    this.setReport(measureReport);
  }
}
