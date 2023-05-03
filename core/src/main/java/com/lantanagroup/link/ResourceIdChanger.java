package com.lantanagroup.link;

import org.apache.commons.codec.digest.DigestUtils;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ResourceIdChanger {
  private Bundle bundle;
  private Logger logger = LoggerFactory.getLogger(ResourceIdChanger.class);

  private ResourceIdChanger(Bundle bundle) {
    this.bundle = bundle;
  }

  /**
   * Creates a new instance of ResourceIdChanger and calls `changeIds()`.
   *
   * @param bundle The bundle to fix IDs to resources for
   */
  public static void changeIds(Bundle bundle) {
    ResourceIdChanger changer = new ResourceIdChanger(bundle);
    changer.changeIds();
  }

  private String getNewId(String rId) {
    String newId = rId.replace(Constants.UuidPrefix, "");
    if (newId.length() > 64) {
      newId = "hash-" + DigestUtils.sha1Hex(newId);
    }
    return newId;
  }

  /**
   * Finds each resource within the Bundle that has an invalid ID,
   * assigns a new ID to the resource that is based on a hash of the original
   * ID. Finds any references to those invalid IDs and updates them.
   */
  public void changeIds() {
    // Find resources that have invalid IDs
    List<Bundle.BundleEntryComponent> invalidEntries = this.bundle.getEntry().stream()
            .filter(e -> e.getResource() != null && e.getResource().getIdElement() != null && e.getResource().getIdElement().getIdPart() != null &&
                    (e.getResource().getIdElement().getIdPart().length() > 64 || e.getResource().getIdElement().getIdPart().contains(Constants.UuidPrefix)))
            .collect(Collectors.toList());
    // Create a map where key = old id, value = new id (a hash of the old id)
    Map<IdType, IdType> newIds = invalidEntries.stream().map(Bundle.BundleEntryComponent::getResource).map(res -> {
      IdType rId = res.getIdElement();

      String newId = getNewId(rId.getIdPart());
      return new IdType[]{rId, new IdType(res.getResourceType().toString(), newId)};
    }).collect(Collectors.toMap(e -> e[0], e -> e[1]));

    logger.debug("Found {} invalid entries", invalidEntries.size());

    // For each resource with an invalid id, update the resource with the new hashed id
    // and find any references to the old id and update those
    invalidEntries.forEach(entry -> {
      Resource res = entry.getResource();
      IdType invalidId = res.getIdElement();
      String invalidIdRef = res.getResourceType().toString() + "/" + invalidId.getIdPart();
      IdType newId = newIds.get(invalidId);
      String newIdRef = res.getResourceType().toString() + "/" + newId.getIdPart();

      // logger.debug(String.format("Updating invalid %s ID from %s to %s", res.getResourceType(), invalidId.getIdPart(), newId.getIdPart()));

      // Update resource with the new hashed id
      res.setIdElement(newId);
      DomainResource resource = (DomainResource) res;

      resource.addExtension(Constants.OriginalResourceIdExtension, new StringType(invalidId.getIdPart()));

      // Update bundle entry if it has a request/url that matches the old ID to reference the new ID
      if (entry.hasRequest() && entry.getRequest().hasUrl() && entry.getRequest().getUrl().equals(invalidIdRef)) {
        entry.getRequest().setUrl(newIdRef);
      }
    });

    List<Reference> references = FhirScanner.findReferences(bundle);

    // Find references that are invalid
    references.stream().filter(r -> {
      if (r.getReference() == null) return false;
      String[] refParts = r.getReference().split("/");
      if (refParts.length != 2) return false;                 // Skip canonical references
      if (refParts[1].length() <= 64 && !refParts[1].contains(Constants.UuidPrefix))
        return false;           // Skip references that aren't invalid
      return true;
    }).forEach(ref -> {
      String origRef = ref.getReference();
      String[] refParts = ref.getReference().split("/");
      String newId = getNewId(refParts[1]);
      ref.setReference(refParts[0] + "/" + newId);
      ref.addExtension(Constants.OriginalResourceIdExtension, new StringType(origRef));
    });
  }
}
