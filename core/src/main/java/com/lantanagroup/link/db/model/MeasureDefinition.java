package com.lantanagroup.link.db.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Bundle;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MeasureDefinition {
  private String id = UUID.randomUUID().toString();
  private String measureId;
  private Bundle bundle;
  private Date lastUpdated = new Date();
}
