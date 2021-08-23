package com.lantanagroup.link;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.BundleUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpResponseException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.swing.text.html.Option;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

public class FhirHelper {
  private static final Logger logger = LoggerFactory.getLogger(FhirHelper.class);
  private static final String NAME = "name";
  private static final String SUBJECT = "sub";
  private static final String REPORT_BUNDLE_TAG = "report-bundle";
  private static final String DOCUMENT_REFERENCE_VERSION_URL = "https://www.cdc.gov/nhsn/fhir/nhsnlink/StructureDefinition/nhsnlink-report-version";
  public static final String ORIG_ID_EXT_URL = "https://www.cdc.gov/nhsn/fhir/nhsnlink/StructureDefinition/nhsnlink-original-id";

  public static void recordAuditEvent (HttpServletRequest request, IGenericClient fhirClient, DecodedJWT jwt, AuditEventTypes type, String outcomeDescription) {
    AuditEvent auditEvent = new AuditEvent();

    switch (type) {
      case Export:
        auditEvent.setType(new Coding(null, "export", null));
        break;
      case Generate:
        auditEvent.setType(new Coding(null, "generate", null));
        break;
      case Send:
        auditEvent.setType(new Coding(null, "send", null));
        break;
      case SearchLocations:
        auditEvent.setType(new Coding(null, "search-locations", null));
        break;
      case InitiateQuery:
        auditEvent.setType(new Coding(null, "initiate-query", null));
      case SearchReports:
        auditEvent.setType(new Coding(null, "search-reports", null));
    }

    auditEvent.setAction(AuditEvent.AuditEventAction.E);
    auditEvent.setRecorded(new Date());
    auditEvent.setOutcome(AuditEvent.AuditEventOutcome._0);
    auditEvent.setOutcomeDesc(outcomeDescription);
    List<AuditEvent.AuditEventAgentComponent> agentList = new ArrayList<>();
    AuditEvent.AuditEventAgentComponent agent = new AuditEvent.AuditEventAgentComponent();
    agent.setRequestor(false);

    String payload = jwt.getPayload();
    byte[] decodedBytes = Base64.getDecoder().decode(payload);
    String decodedString = new String(decodedBytes);

    JsonObject jsonObject = new JsonParser().parse(decodedString).getAsJsonObject();
    if (jsonObject.has(NAME)) {
      agent.setName(jsonObject.get(NAME).toString());
    }
    if (jsonObject.has(SUBJECT)) {
      agent.setAltId(jsonObject.get(SUBJECT).toString());
    }

    String remoteAddress = request.getRemoteAddr() != null ? (request.getRemoteHost() != null ? request.getRemoteAddr() + "(" + request.getRemoteHost() + ")" : request.getRemoteAddr()) : "";
    if (remoteAddress != null) {
      agent.setNetwork(new AuditEvent.AuditEventAgentNetworkComponent().setAddress(remoteAddress));
    }

    if (jsonObject.has("aud")) {
      String aud = jsonObject.get("aud").getAsString();
      Identifier identifier = new Identifier().setValue(aud);
      agent.setLocation(new Reference().setIdentifier(identifier));
    }
    agentList.add(agent);
    auditEvent.setAgent(agentList);


    try {
      MethodOutcome outcome = fhirClient.create()
              .resource(auditEvent)
              .prettyPrint()
              .encodedJson()
              .execute();

      IIdType id = outcome.getId();
      logger.info("AuditEvent LOGGED: " + id.getValue());
    } catch (Exception ex) {
      logger.error("Failed to record AuditEvent", ex);
    }
  }

  public static DocumentReference getDocumentReference(IGenericClient client, String reportId) throws HttpResponseException{
    Bundle documentReferences = client.search()
      .forResource("DocumentReference")
      .where(DocumentReference.IDENTIFIER.exactly().identifier(reportId))
      .returnBundle(Bundle.class)
      .cacheControl(new CacheControlDirective().setNoCache(true))
      .execute();


    if (!documentReferences.hasEntry() || documentReferences.getEntry().size() != 1) {
      throw new HttpResponseException(404, String.format("Report with id %s does not exist", reportId));
    }

    return (DocumentReference) documentReferences.getEntry().get(0).getResource();
  }

  public static MeasureReport getMeasureReport(IGenericClient client, String reportId){
    return client.read()
            .resource(MeasureReport.class)
            .withId(reportId)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();
  }

  public static Extension createVersionExtension(String value){
    return new Extension(DOCUMENT_REFERENCE_VERSION_URL, new StringType(value));
  }

  /**
   *
   * @param documentReference - DocumentReference whose minor version is to be incremented
   * @return - the DocumentReference with the minor version incremented by 1
   */
  public static DocumentReference incrementMinorVersion(DocumentReference documentReference){

    if(documentReference.getExtensionByUrl(DOCUMENT_REFERENCE_VERSION_URL) == null){
      documentReference.addExtension(createVersionExtension("0.1"));
    }
    else {
      String version = documentReference
              .getExtensionByUrl(DOCUMENT_REFERENCE_VERSION_URL)
              .getValue().toString();

      documentReference.getExtensionByUrl(DOCUMENT_REFERENCE_VERSION_URL)
              .setValue(new StringType(version.substring(0, version.indexOf(".") + 1) + (Integer.parseInt(version.substring(version.indexOf(".") + 1)) + 1)));
    }

    return documentReference;
  }

  /**
   *
   * @param documentReference - DocumentReference whose major version is to be incremented
   * @return - the DocumentReference with the major version incremented by 1
   */
  public static DocumentReference incrementMajorVersion(DocumentReference documentReference){
    if(documentReference.getExtensionByUrl(DOCUMENT_REFERENCE_VERSION_URL) == null){
      documentReference.addExtension(createVersionExtension("1.0"));
    }
    else {
      String version = documentReference
              .getExtensionByUrl(DOCUMENT_REFERENCE_VERSION_URL)
              .getValue().toString();

      version = version.substring(0, version.indexOf("."));
      documentReference.getExtensionByUrl(DOCUMENT_REFERENCE_VERSION_URL)
              .setValue(new StringType((Integer.parseInt(version) + 1) + ".0"));
    }
    return documentReference;
  }

  public static Bundle bundleMeasureReport (MeasureReport measureReport, IGenericClient fhirServer) {
    Meta meta = new Meta();
    Coding tag = meta.addTag();
    tag.setCode(REPORT_BUNDLE_TAG);
    tag.setSystem(Constants.MainSystem);

    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.COLLECTION);
    bundle.setMeta(meta);
    bundle.addEntry().setResource(measureReport);

    List<String> resourceReferences = new ArrayList<>();

    for (Reference evaluatedResource : measureReport.getEvaluatedResource()) {
      if (!evaluatedResource.hasReference()) continue;

      if (!evaluatedResource.getReference().startsWith("#")) {
        resourceReferences.add(evaluatedResource.getReference());
      }
    }

    Bundle patientBundle = generateBundle(resourceReferences);

    Bundle patientBundleResponse = fhirServer.transaction().withBundle(patientBundle).execute();

    patientBundleResponse.getEntry().parallelStream().forEach(entry -> {
      bundle.addEntry().setResource((Resource) entry.getResource());
    });

    return bundle;
  }

  private static Bundle generateBundle (List<String> resourceReferences) {
    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.TRANSACTION);
    resourceReferences.parallelStream().forEach(reference -> {
      String[] referenceSplit = reference.split("/");
      bundle.addEntry().getRequest().setMethod(Bundle.HTTPVerb.GET).setUrl(referenceSplit[0] + "/" + referenceSplit[1]);
    });
    return bundle;
  }

  public static List<IBaseResource> getAllPages (Bundle bundle, IGenericClient fhirStoreClient, FhirContext ctx) {
    List<IBaseResource> bundles = new ArrayList<>();
    bundles.addAll(BundleUtil.toListOfResources(ctx, bundle));

    // Load the subsequent pages
    while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
      bundle = fhirStoreClient
              .loadPage()
              .next(bundle)
              .execute();
      logger.info("Adding next page of bundles...");
      bundles.addAll(BundleUtil.toListOfResources(ctx, bundle));
    }
    return bundles;
  }

  public static void changeIds (Bundle bundle) {
    List<Reference> references = findReferences(bundle)
            .stream()
            .filter(r -> r.getReference() != null && r.getReference().split("/").length == 2)
            .collect(Collectors.toList());
    HashMap<String, String> ids = new HashMap<>();

    for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
      if (entry.getResource() == null || StringUtils.isEmpty(entry.getResource().getId())) continue;
      ids.put(entry.getResource().getResourceType().toString() + "/" + entry.getResource().getId(), UUID.randomUUID().toString());
    }

    for (Reference r : references) {
      String[] parts = r.getReference() != null ? r.getReference().split("/") : null;
      ResourceType resourceType = ResourceType.fromCode(parts[0]);
      String currentId = parts[1];

      if (parts == null || ids.containsKey(r.getReference())) continue;

      Boolean found = bundle.getEntry().stream()
              .filter(e -> {
                return e.getResource() != null &&
                        e.getResource().getResourceType() == resourceType &&
                        e.getResource().getIdElement() != null &&
                        e.getResource().getIdElement().getIdPart() != null &&
                        e.getResource().getIdElement().getIdPart().equals(currentId);
              })
              .findAny()
              .isPresent();

      if (found) {
        ids.put(r.getReference(), UUID.randomUUID().toString());
      }
    }

    for (String ref : ids.keySet()) {
      String newId = ids.get(ref);
      String[] parts = ref.split("/");
      List<Reference> matchingRefs = references.stream()
              .filter(r -> r.getReference().equals(ref))
              .collect(Collectors.toList());

      matchingRefs.forEach(mr -> {
        mr.setReference(parts[0] + "/" + newId);
      });

      Bundle.BundleEntryComponent foundEntry = bundle.getEntry().stream()
              .filter(e -> {
                return e.getResource() != null &&
                        e.getResource().getResourceType() == ResourceType.fromCode(parts[0]) &&
                        e.getResource().getIdElement() != null &&
                        e.getResource().getIdElement().getIdPart() != null &&
                        e.getResource().getIdElement().getIdPart().equals(parts[1]);
              })
              .findFirst()
              .get();

      foundEntry.getResource().setId(newId);
    }
  }

  /**
   * Finds any instance (recursively) of a Reference within the specified object
   *
   * @param obj The object to search
   * @return A list of Reference instances found in the object
   */
  public static List<Reference> findReferences (Object obj) {
    List<Reference> references = new ArrayList<>();
    scanInstance(obj, Reference.class, Collections.newSetFromMap(new IdentityHashMap<>()), references);
    return references;
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
  private static <T> void scanInstance (Object objectToScan, Class<T> lookingFor, Set<? super Object> scanned, Collection<? super T> results) {
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
          if (!declaredField.trySetAccessible()) {
            // either throw error, skip, or use more black magic like Unsafe class to make field accessible anyways.
            continue; // I will just skip it, it's probably some internal one.
          }
          try {
            scanInstance(declaredField.get(objectToScan), lookingFor, scanned, results);
          } catch (IllegalAccessException ignored) {
            continue;
          }
        }
        currentClass = currentClass.getSuperclass();
      }
    }
  }

  public static void addEntriesToBundle(Bundle source, Bundle destination) {
    if (source == null) return;

    List<Bundle.BundleEntryComponent> sourceEntries = source.getEntry();

    for (Bundle.BundleEntryComponent sourceEntry : sourceEntries) {
      if (sourceEntry.getResource() == null || sourceEntry.getResource().getIdElement() == null || sourceEntry.getResource().getId() == null) continue;

      List<Bundle.BundleEntryComponent> destEntries = new ArrayList<>(destination.getEntry());
      Optional<Bundle.BundleEntryComponent> found =
              destEntries.stream()
                      .filter(n ->
                              n.getResource().getResourceType() == sourceEntry.getResource().getResourceType() &&
                                      n.getResource().getIdElement().getIdPart() == sourceEntry.getResource().getIdElement().getIdPart())
                      .findFirst();

      // Only add the resource to the bundle if it doesn't already exist
      if (found.isPresent()) {
        logger.debug(String.format("Resource %s/%s is a duplicate, skipping...", sourceEntry.getResource().getResourceType(), sourceEntry.getResource().getIdElement().getIdPart()));
      } else {
        destination.addEntry()
                .setResource(sourceEntry.getResource())
                .getRequest()
                .setMethod(Bundle.HTTPVerb.PUT)
                .setUrl(sourceEntry.getResource().getResourceType().toString() + "/" + sourceEntry.getResource().getIdElement().getIdPart());
      }
    }
  }

  public static Bundle.BundleEntryComponent findEntry(Bundle bundle, ResourceType resourceType, String id) {
    Optional<Bundle.BundleEntryComponent> found = bundle.getEntry().stream().filter(e ->
                    e.getResource().getResourceType() == resourceType &&
                    e.getResource().getIdElement().getIdPart().equals(id))
            .findFirst();
    return found.isPresent() ? found.get() : null;
  }

  /**
   * Finds each resource within the Bundle that has an invalid ID,
   * assigns a new ID to the resource that is based on a hash of the original
   * ID. Finds any references to those invalid IDs and updates them.
   * @param bundle The bundle to fix IDs to resources for
   */
  public static void fixResourceIds(Bundle bundle) {
    // Find resources that invalid invalid IDs
    List<Resource> invalidResourceIds = bundle.getEntry().stream()
            .filter(e -> e.getResource().getIdElement().getIdPart().length() > 64)
            .map(e -> e.getResource())
            .collect(Collectors.toList());
    // Create a map where key = old id, value = new id (a hash of the old id)
    Map<IdType, IdType> newIds = invalidResourceIds.stream().map(res -> {
      IdType rId = res.getIdElement();
      return new IdType[] { rId, new IdType(res.getResourceType().toString(), "HASH" + String.valueOf(rId.getIdPart().hashCode()))};
    }).collect(Collectors.toMap(e -> e[0], e-> e[1]));

    // Find all references within the bundle
    List<Reference> references = findReferences(bundle);

    // For each resource with an invalid id, update the resource with the new hashed id
    // and find any references to the old id and update those
    invalidResourceIds.forEach(res -> {
      IdType invalidId = res.getIdElement();
      String invalidIdRef = res.getResourceType().toString() + "/" + invalidId.getIdPart();
      IdType newId = newIds.get(invalidId);
      String newIdRef = res.getResourceType().toString() + "/" + newId.getIdPart();

      logger.debug(String.format("Updating invalid %s ID from %s to %s", res.getResourceType(), invalidId.getIdPart(), newId.getIdPart()));

      // Update resource with the new hashed id
      res.setIdElement(newId);
      DomainResource resource = (DomainResource) res;
      resource.addExtension(ORIG_ID_EXT_URL, new StringType(invalidId.getIdPart()));

      // Find references to the old id and update it
      List<Reference> invalidIdReferences = references.stream()
                      .filter(ref -> ref.getReference() != null && ref.getReference().equals(invalidIdRef))
                      .collect(Collectors.toList());

      logger.debug(String.format("Found %s references to %s to update to %s.", invalidIdReferences.size(), invalidIdRef, newId.getIdPart()));

      invalidIdReferences
              .forEach(ref -> {
                String oldIdRef = ref.getReference();
                ref.setReference(newIdRef).addExtension(ORIG_ID_EXT_URL, new StringType(oldIdRef));
              });

      // Update bundle entry's that have a request/url that matches the old ID to reference the new ID
      bundle.getEntry().stream()
              .filter(entry ->
                      entry.getRequest() != null &&
                      entry.getRequest().getUrl() != null &&
                      entry.getRequest().getUrl().equals(invalidIdRef))
              .forEach(entry -> entry.getRequest().setUrl(newIdRef));
    });
  }

  public enum AuditEventTypes {
    Generate,
    Export,
    Send,
    SearchLocations,
    InitiateQuery,
    SearchReports
  }
}
