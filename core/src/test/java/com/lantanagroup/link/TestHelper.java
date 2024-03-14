package com.lantanagroup.link;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.Bundle;

import java.util.Objects;

public class TestHelper {
  private static final FhirContext ctx = FhirContext.forR4();

  public static Bundle getBundle(String resourcePath) {
    return ctx.newJsonParser().parseResource(
            Bundle.class,
            Objects.requireNonNull(TestHelper.class.getClassLoader().getResourceAsStream(resourcePath)));
  }
}
