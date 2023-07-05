package com.lantanagroup.link.db.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConceptMap {
  private String id;
  private List<String> contexts = new ArrayList<>();
  private org.hl7.fhir.r4.model.ConceptMap conceptMap;
}
