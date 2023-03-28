package com.lantanagroup.link.db.model;

import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.Date;

@Getter
@Setter
public class PatientData {
  private String id = (new ObjectId()).toString();
  private String patientId;
  private String resourceType;
  private String resourceId;
  private Date retrieved;
  private IBaseResource resource;
}
