package com.lantanagroup.link.validation;

import org.hl7.fhir.r5.elementmodel.Element;
import org.hl7.fhir.r5.model.ElementDefinition;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.model.ValueSet;
import org.hl7.fhir.r5.utils.validation.IResourceValidator;
import org.hl7.fhir.r5.utils.validation.IValidationPolicyAdvisor;
import org.hl7.fhir.r5.utils.validation.constants.BindingKind;
import org.hl7.fhir.r5.utils.validation.constants.CodedContentValidationPolicy;
import org.hl7.fhir.r5.utils.validation.constants.ContainedReferenceValidationPolicy;
import org.hl7.fhir.r5.utils.validation.constants.ReferenceValidationPolicy;

import java.util.List;

public class PolicyAdvisor implements IValidationPolicyAdvisor {

  @Override
  public ReferenceValidationPolicy policyForReference(IResourceValidator iResourceValidator, Object o, String s, String s1) {
    return ReferenceValidationPolicy.IGNORE;
  }

  @Override
  public ContainedReferenceValidationPolicy policyForContained(IResourceValidator iResourceValidator, Object o, String s, String s1, Element.SpecialElement specialElement, String s2, String s3) {
    return ContainedReferenceValidationPolicy.IGNORE;
  }

  @Override
  public CodedContentValidationPolicy policyForCodedContent(IResourceValidator iResourceValidator, Object o, String s, ElementDefinition elementDefinition, StructureDefinition structureDefinition, BindingKind bindingKind, ValueSet valueSet, List<String> list) {
    return CodedContentValidationPolicy.IGNORE;
  }
}
