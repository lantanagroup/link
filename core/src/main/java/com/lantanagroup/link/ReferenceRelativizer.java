package com.lantanagroup.link;

import ca.uhn.fhir.context.FhirContext;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;

import java.util.List;

public class ReferenceRelativizer {
  private final FhirContext fhirContext;

  public ReferenceRelativizer(FhirContext fhirContext) {
    this.fhirContext = fhirContext;
  }

  /**
   * Removes the base URL portion of references in the specified resource.
   * If a non-null expected base URL is specified, relativizes only matching references.
   * Otherwise, relativizes all references.
   */
  public void relativize(IBaseResource resource, String expectedBaseUrl) {
    String cleanExpectedBaseUrl = expectedBaseUrl == null ? null : StringUtils.stripEnd(expectedBaseUrl, "/");
    List<Reference> references = fhirContext.newTerser().getAllPopulatedChildElementsOfType(resource, Reference.class);
    for (Reference reference : references) {
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
