package com.lantanagroup.link.db.model;

import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.hl7.fhir.r4.model.Bundle;

import java.util.Date;

@Getter
@Setter
public class MeasureDefinition {
  private String id = (new ObjectId()).toString();
  private String measureId;
  private Bundle bundle;
  private Date lastUpdated = new Date();
}
