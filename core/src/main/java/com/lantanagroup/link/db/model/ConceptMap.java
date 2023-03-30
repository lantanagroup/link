package com.lantanagroup.link.db.model;

import lombok.Getter;
import lombok.Setter;

/*
Because HAPI can't deserialize _id from MongoDB into "id" used by the models, this wrapper class
is needed
 */
@Getter
@Setter
public class ConceptMap {
  public String id;
  public String name;
  public org.hl7.fhir.r4.model.ConceptMap resource;

  public ConceptMap() {

  }

  public ConceptMap(org.hl7.fhir.r4.model.ConceptMap conceptMap) {
    this.setResource(conceptMap);
    if (conceptMap.hasId()) {
      this.setId(conceptMap.getIdElement().getIdPart());
    }
    this.setName(conceptMap.getName());
  }
}
