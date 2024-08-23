package com.lantanagroup.link;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.FhirTerser;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReferenceRelativizer {
  private final FhirContext fhirContext;

  public ReferenceRelativizer(FhirContext fhirContext) {
    this.fhirContext = fhirContext;
  }

  private List<Reference> getReferences(IBaseResource resource) {
    List<IBaseResource> resources;
    if (resource instanceof Bundle) {
      resources = ((Bundle) resource).getEntry().stream()
              .map(Bundle.BundleEntryComponent::getResource)
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
    } else {
      resources = List.of(resource);
    }
    FhirTerser fhirTerser = fhirContext.newTerser();
    return resources.stream()
            .flatMap(_resource -> fhirTerser.getAllPopulatedChildElementsOfType(_resource, Reference.class).stream())
            .collect(Collectors.toList());
  }

  /**
   * Removes the base URL portion of references in the specified resource.
   * If a non-null expected base URL is specified, relativizes only matching references.
   * Otherwise, relativizes all references.
   */
  public void relativize(IBaseResource resource, String expectedBaseUrl) {
    String cleanExpectedBaseUrl = expectedBaseUrl == null ? null : StringUtils.stripEnd(expectedBaseUrl, "/");
    for (Reference reference : getReferences(resource)) {
      String referenceElement = reference.getReference();
      if (StringUtils.isEmpty(referenceElement)) {
        continue;
      }
      IdType referenceId = new IdType(referenceElement);
      String referenceBaseUrl = referenceId.getBaseUrl();
      if (referenceBaseUrl == null) {
        continue;
      }
      if (cleanExpectedBaseUrl == null || StringUtils.equals(referenceBaseUrl, cleanExpectedBaseUrl)) {
        reference.addExtension()
                .setUrl(Constants.OriginalElementValueExtension)
                .setValue(new StringType(referenceElement));
        reference.setReference(referenceId.toUnqualified().getValue());
      }
    }
  }

  /**
   * Removes the base URL portion of references in the specified resource.
   */
  public void relativize(IBaseResource resource) {
    relativize(resource, null);
  }
}
