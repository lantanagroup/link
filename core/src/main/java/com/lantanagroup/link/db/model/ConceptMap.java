package com.lantanagroup.link.db.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/*
Because HAPI can't deserialize _id from MongoDB into "id" used by the models, this wrapper class
is needed
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConceptMap {
  private String id;
  private String name;
  private org.hl7.fhir.r4.model.ConceptMap conceptMap;
  private List<String> contexts = new ArrayList<>();
}
