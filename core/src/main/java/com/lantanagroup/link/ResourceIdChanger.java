package com.lantanagroup.link;

import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

public class ResourceIdChanger {
  public static final String ORIG_ID_EXT_URL = "https://www.cdc.gov/nhsn/fhir/nhsnlink/StructureDefinition/nhsnlink-original-id";
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

  /**
   * Finds any instance (recursively) of a Reference within the specified object
   * @param obj The object to search
   * @return A list of Reference instances found in the object
   */
  public static List<Reference> findReferences(Object obj) {
    List<Reference> references = new ArrayList<>();
    scanInstance(obj, Reference.class, Collections.newSetFromMap(new IdentityHashMap<>()), references);
    return references;
  }

  /**
   * Finds any instance (recursively) of a CodeableConcept or Coding within the specified object
   *
   * @param obj The object to search
   * @return A list of CodeableConcept or Coding instances found in the object
   */
  public static List<Coding> findCodings(Object obj) {
    List<Coding> codes = new ArrayList<>();
    scanInstance(obj, Coding.class, Collections.newSetFromMap(new IdentityHashMap<>()), codes);
    return codes;
  }

  /**
   * Scans an object recursively to find any instances of the specified type
   *
   * @param objectToScan The object to scan
   * @param lookingFor   The class/type to find instances of
   * @param scanned      A pre-initialized set that is used internally to determine what has already been scanned to avoid endless recursion on self-referencing objects
   * @param results      A pre-initialized collection/list that will be populated with the results of the scan
   * @param <T>          The type of class to look for instances of that must match the initialized results collection
   * @implNote Found this code online from https://stackoverflow.com/questions/57758392/is-there-are-any-way-to-get-all-the-instances-of-type-x-by-reflection-utils
   */
  private static <T> void scanInstance(Object objectToScan, Class<T> lookingFor, Set<? super Object> scanned, Collection<? super T> results) {
    if (objectToScan == null) {
      return;
    }
    if (!scanned.add(objectToScan)) { // to prevent any endless scan loops
      return;
    }
    // you might need some extra code if you want to correctly support scanning for primitive types
    if (lookingFor.isInstance(objectToScan)) {
      results.add(lookingFor.cast(objectToScan));
      // either return or continue to scan of target object might contains references to other objects of this type
    }
    // we won't find anything intresting in Strings, and it is pretty popular type
    if (objectToScan instanceof String) {
      return;
    }
    // LINK-805: avoid illegal reflective access
    // consider adding checks for other types we don't want to recurse into
    else if (objectToScan instanceof Enum) {
      return;
    }
    // basic support for popular java types to prevent scanning too much of java internals in most common cases, but might cause
    // side-effects in some cases
    else if (objectToScan instanceof Iterable) {
      ((Iterable<?>) objectToScan).forEach(obj -> scanInstance(obj, lookingFor, scanned, results));
    } else if (objectToScan instanceof Map) {
      ((Map<?, ?>) objectToScan).forEach((key, value) -> {
        scanInstance(key, lookingFor, scanned, results);
        scanInstance(value, lookingFor, scanned, results);
      });
    }
    // remember about arrays, if you want to support primitive types remember to use Array class instead.
    else if (objectToScan instanceof Object[]) {
      int length = Array.getLength(objectToScan);
      for (int i = 0; i < length; i++) {
        scanInstance(Array.get(objectToScan, i), lookingFor, scanned, results);
      }
    } else if (objectToScan.getClass().isArray()) {
      return; // primitive array
    } else {
      Class<?> currentClass = objectToScan.getClass();
      while (currentClass != Object.class) {
        for (Field declaredField : currentClass.getDeclaredFields()) {
          // skip static fields
          if (Modifier.isStatic(declaredField.getModifiers())) {
            continue;
          }
          // skip primitives, to prevent wrapping of "int" to "Integer" and then trying to scan its "value" field and loop endlessly.
          if (declaredField.getType().isPrimitive()) {
            return;
          }
          try {
            if (!declaredField.trySetAccessible()) {
              // either throw error, skip, or use more black magic like Unsafe class to make field accessible anyways.
              continue; // I will just skip it, it's probably some internal one.
            }
            scanInstance(declaredField.get(objectToScan), lookingFor, scanned, results);
          } catch (IllegalAccessException | SecurityException ignored) {
            continue;
          }
        }
        currentClass = currentClass.getSuperclass();
      }
    }
  }

  private String getNewId(String rId) {
    String newId = rId.replace(Constants.UuidPrefix, "");
    if (newId.length() > 64) {
      newId = "hash-" + Integer.toHexString(newId.hashCode());
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


    // For each resource with an invalid id, update the resource with the new hashed id
    // and find any references to the old id and update those
    invalidEntries.forEach(entry -> {
      Resource res = entry.getResource();
      IdType invalidId = res.getIdElement();
      String invalidIdRef = res.getResourceType().toString() + "/" + invalidId.getIdPart();
      IdType newId = newIds.get(invalidId);
      String newIdRef = res.getResourceType().toString() + "/" + newId.getIdPart();

      logger.debug(String.format("Updating invalid %s ID from %s to %s", res.getResourceType(), invalidId.getIdPart(), newId.getIdPart()));

      // Update resource with the new hashed id
      res.setIdElement(newId);
      DomainResource resource = (DomainResource) res;

      resource.addExtension(ORIG_ID_EXT_URL, new StringType(invalidId.getIdPart()));

      // Update bundle entry if it has a request/url that matches the old ID to reference the new ID
      if (entry.hasRequest() && entry.getRequest().hasUrl() && entry.getRequest().getUrl().equals(invalidIdRef)) {
        entry.getRequest().setUrl(newIdRef);
      }
    });

    List<Reference> references = findReferences(bundle);

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
      ref.addExtension().setUrl(ORIG_ID_EXT_URL).setValue(new StringType(origRef));
    });
  }
}
