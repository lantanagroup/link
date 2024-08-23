package com.lantanagroup.link;

import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertArrayEquals;

public class ReferenceRelativizerTests {
  private final ReferenceRelativizer referenceRelativizer;
  private final List<String> references = List.of(
          "Patient/1",
          "https://foo/fhir/Patient/2",
          "https://bar/fhir/Patient/3");
  private ListResource list;

  public ReferenceRelativizerTests() {
    referenceRelativizer = new ReferenceRelativizer(FhirContextProvider.getFhirContext());
  }

  @Before
  public void initializeList() {
    list = new ListResource();
    for (String reference : references) {
      list.addEntry().getItem().setReference(reference);
    }
  }

  private void doTest(String... expectedReferences) {
    String[] actualReferences = list.getEntry().stream()
            .map(ListResource.ListEntryComponent::getItem)
            .map(Reference::getReference)
            .toArray(String[]::new);
    assertArrayEquals(expectedReferences, actualReferences);
  }

  @Test
  public void testRelativizeWithNonNullExpectedBaseUrl() {
    referenceRelativizer.relativize(list, "https://foo/fhir");
    doTest("Patient/1", "Patient/2", "https://bar/fhir/Patient/3");
  }

  @Test
  public void testRelativizeWithNonNullExpectedBaseUrlAndTrailingSlash() {
    referenceRelativizer.relativize(list, "https://foo/fhir/");
    doTest("Patient/1", "Patient/2", "https://bar/fhir/Patient/3");
  }

  @Test
  public void testRelativizeWithNullExpectedBaseUrl() {
    referenceRelativizer.relativize(list, null);
    doTest("Patient/1", "Patient/2", "Patient/3");
  }
}
