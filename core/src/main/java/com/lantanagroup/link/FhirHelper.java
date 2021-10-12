package com.lantanagroup.link;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
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
import java.util.*;

public class FhirHelper {
  private static final Logger logger = LoggerFactory.getLogger(FhirHelper.class);
  private static final String NAME = "name";
  private static final String SUBJECT = "sub";
  private static final String REPORT_BUNDLE_TAG = "report-bundle";
  private static final String DOCUMENT_REFERENCE_VERSION_URL = "https://www.cdc.gov/nhsn/fhir/nhsnlink/StructureDefinition/nhsnlink-report-version";

  public static void recordAuditEvent(HttpServletRequest request, IGenericClient fhirClient, DecodedJWT jwt, AuditEventTypes type, String outcomeDescription) {
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

  /**
   * Gets resources of the specified type for the patient given the criterion
   *
   * @param fhirClient   The FHIR client to use for the HTTP interaction
   * @param criterion    The criteria to use when searching for the specific resource type. Example: Condition.SUBJECT.hasId(patientId)
   * @param resourceType The type of resource to search for
   * @return
   */
  public static Bundle getPatientResources(IGenericClient fhirClient, ICriterion<ReferenceClientParam> criterion, String resourceType) {
    return fhirClient
            .search()
            .forResource(resourceType)
            .where(criterion)
            .returnBundle(Bundle.class)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();
  }

  public static String getName(List<HumanName> names) {
    String firstName = "", lastName = "";
    if (names.size() > 0 && names.get(0) != null) {
      if (StringUtils.isNotEmpty(names.get(0).getText())) {
        return names.get(0).getText();
      }
      if (names.get(0).getGiven().size() > 0 && names.get(0).getGiven().get(0) != null) {
        firstName = names.get(0).getGiven().get(0).toString();
      }
      if (names.get(0).getFamily() != null) {
        lastName = names.get(0).getFamily();
      }
    } else {
      return "Unknown";
    }

    if (StringUtils.isNotEmpty(firstName) && StringUtils.isNotEmpty(lastName)) {
      return (firstName + " " + lastName).replace("\"", "");
    } else if (StringUtils.isNotEmpty(lastName)) {
      return lastName;
    } else if (StringUtils.isNotEmpty(firstName)) {
      return firstName;
    }

    return "Unknown";
  }

  public static DocumentReference getDocumentReference(IGenericClient client, String reportId) throws HttpResponseException {
    Bundle documentReferences = client.search()
            .forResource(DocumentReference.class)
            .where(DocumentReference.IDENTIFIER.exactly().identifier(reportId))
            .returnBundle(Bundle.class)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();

    if (!documentReferences.hasEntry() || documentReferences.getEntry().size() != 1) {
      throw new HttpResponseException(404, String.format("Report with id %s does not exist", reportId));
    }

    return (DocumentReference) documentReferences.getEntry().get(0).getResource();
  }

  public static MeasureReport getMeasureReport(IGenericClient client, String reportId) {
    return client.read()
            .resource(MeasureReport.class)
            .withId(reportId)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();
  }

  public static Extension createVersionExtension(String value) {
    return new Extension(DOCUMENT_REFERENCE_VERSION_URL, new StringType(value));
  }

  /**
   * Increments the minor version of the specified report
   * @param documentReference - DocumentReference whose minor version is to be incremented
   * @return - the DocumentReference with the minor version incremented by 1
   */
  public static DocumentReference incrementMinorVersion(DocumentReference documentReference) {

    if (documentReference.getExtensionByUrl(DOCUMENT_REFERENCE_VERSION_URL) == null) {
      documentReference.addExtension(createVersionExtension("0.1"));
    } else {
      String version = documentReference
              .getExtensionByUrl(DOCUMENT_REFERENCE_VERSION_URL)
              .getValue().toString();

      documentReference.getExtensionByUrl(DOCUMENT_REFERENCE_VERSION_URL)
              .setValue(new StringType(version.substring(0, version.indexOf(".") + 1) + (Integer.parseInt(version.substring(version.indexOf(".") + 1)) + 1)));
    }

    return documentReference;
  }

  /**
   * @param documentReference - DocumentReference whose major version is to be incremented
   * @return - the DocumentReference with the major version incremented by 1
   */
  public static DocumentReference incrementMajorVersion(DocumentReference documentReference) {
    if (documentReference.getExtensionByUrl(DOCUMENT_REFERENCE_VERSION_URL) == null) {
      documentReference.addExtension(createVersionExtension("1.0"));
    } else {
      String version = documentReference
              .getExtensionByUrl(DOCUMENT_REFERENCE_VERSION_URL)
              .getValue().toString();

      version = version.substring(0, version.indexOf("."));
      documentReference.getExtensionByUrl(DOCUMENT_REFERENCE_VERSION_URL)
              .setValue(new StringType((Integer.parseInt(version) + 1) + ".0"));
    }
    return documentReference;
  }

  public static Bundle bundleMeasureReport(MeasureReport measureReport, IGenericClient fhirServer) {
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

  private static Bundle generateBundle(List<String> resourceReferences) {
    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.TRANSACTION);
    resourceReferences.parallelStream().forEach(reference -> {
      String[] referenceSplit = reference.split("/");
      bundle.addEntry().getRequest().setMethod(Bundle.HTTPVerb.GET).setUrl(referenceSplit[0] + "/" + referenceSplit[1]);
    });
    return bundle;
  }

  public static List<IBaseResource> getAllPages(Bundle bundle, IGenericClient fhirStoreClient, FhirContext ctx) {
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

  public static void addEntriesToBundle(Bundle source, Bundle destination) {
    if (source == null) return;

    List<Bundle.BundleEntryComponent> sourceEntries = source.getEntry();

    for (Bundle.BundleEntryComponent sourceEntry : sourceEntries) {
      if (sourceEntry.getResource() == null || sourceEntry.getResource().getIdElement() == null || sourceEntry.getResource().getId() == null)
        continue;

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
   * Gets a Measure for the given DocumentReference, by looking for a Measure that matches the identifier
   * included in the DocumentReference.identifier
   * @param fhirStoreClient
   * @param docRef
   * @return
   */
  public static Measure getMeasure(IGenericClient fhirStoreClient, DocumentReference docRef) {
    if (docRef.getIdentifier().size() > 0) {
      for (Identifier identifier : docRef.getIdentifier()) {
        Bundle matchingMeasures = fhirStoreClient.search()
                .forResource(Measure.class)
                .where(DocumentReference.IDENTIFIER.exactly().systemAndValues(identifier.getSystem(), identifier.getValue()))
                .returnBundle(Bundle.class)
                .summaryMode(SummaryEnum.TRUE)
                .execute();

        if (matchingMeasures.getEntry().size() == 1) {
          return (Measure) matchingMeasures.getEntry().get(0).getResource();
        }
      }
    }

    return null;
  }

  public enum AuditEventTypes {
    Generate,
    ExcludePatients,
    Export,
    Send,
    SearchLocations,
    InitiateQuery,
    SearchReports
  }
}
