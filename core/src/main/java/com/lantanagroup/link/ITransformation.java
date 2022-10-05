package com.lantanagroup.link;

import org.hl7.fhir.r4.model.*;

import java.util.List;

public interface ITransformation {

  List<Coding> applyTransformation(ConceptMap conceptMap, List<Coding> input);

}
