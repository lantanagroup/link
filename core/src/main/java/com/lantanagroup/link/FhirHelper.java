package com.lantanagroup.link;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.util.BundleUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lantanagroup.link.model.PatientReportModel;
import org.apache.commons.lang3.StringUtils;
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

  public static AuditEvent createAuditEvent(HttpServletRequest request, DecodedJWT jwt, AuditEventTypes type, String outcomeDescription) {
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

    return auditEvent;
  }

  public static void recordAuditEvent(HttpServletRequest request, FhirDataProvider fhirClient, DecodedJWT jwt, AuditEventTypes type, String outcomeDescription) {
    AuditEvent auditEvent = createAuditEvent(request, jwt, type, outcomeDescription);

    try {
      MethodOutcome outcome = fhirClient.createOutcome(auditEvent);

      IIdType id = outcome.getId();
      logger.info("AuditEvent LOGGED: " + id.getValue());
    } catch (Exception ex) {
      logger.error("Failed to record AuditEvent", ex);
    }
  }

  /**
   * Retrieves the relevant ID portion of the version of a resource
   *
   * @param version A resource's version URI
   * @return The ID portion of the resource's version
   */
  public static String getIdFromVersion(String version) {
    return version.substring(version.lastIndexOf("fhir/") + 5, version.indexOf("/_history"));
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


  public static Extension createVersionExtension(String value) {
    return new Extension(DOCUMENT_REFERENCE_VERSION_URL, new StringType(value));
  }

  /**
   * Increments the minor version of the specified report
   *
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

  public static Bundle bundleMeasureReport(MeasureReport measureReport, FhirDataProvider fhirProvider, Boolean sendWholeBundle) {
    Meta meta = new Meta();
    Coding tag = meta.addTag();
    tag.setCode(REPORT_BUNDLE_TAG);
    tag.setSystem(Constants.MainSystem);

    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.COLLECTION);
    bundle.setMeta(meta);
    bundle.addEntry().setResource(measureReport);

    if (sendWholeBundle) {
      List<String> resourceReferences = new ArrayList<>();

      for (Reference evaluatedResource : measureReport.getEvaluatedResource()) {
        if (!evaluatedResource.hasReference()) continue;

        if (!evaluatedResource.getReference().startsWith("#")) {
          resourceReferences.add(evaluatedResource.getReference());
        }
      }

      Bundle patientBundle = generateBundle(resourceReferences);

      Bundle patientBundleResponse = fhirProvider.transaction(patientBundle);

      patientBundleResponse.getEntry().parallelStream().forEach(entry -> {
        bundle.addEntry().setResource((Resource) entry.getResource());
      });
    }


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

  public static List<IBaseResource> getAllPages(Bundle bundle, FhirDataProvider fhirDataProvider, FhirContext ctx) {
    List<IBaseResource> bundles = new ArrayList<>();
    bundles.addAll(BundleUtil.toListOfResources(ctx, bundle));

    // Load the subsequent pages
    while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
      bundle = fhirDataProvider
              .getClient()
              .loadPage()
              .next(bundle)
              .execute();
      logger.info("Adding next page of bundles...");
      bundles.addAll(BundleUtil.toListOfResources(ctx, bundle));
    }
    return bundles;
  }

  public static PatientReportModel setPatientFields(Patient patient, Boolean excluded) {
    PatientReportModel report = new PatientReportModel();
    report.setName(FhirHelper.getName(patient.getName()));

    if (patient.getBirthDate() != null) {
      report.setDateOfBirth(Helper.getFhirDate(patient.getBirthDate()));
    }

    if (patient.getGender() != null) {
      report.setSex(patient.getGender().toString());
    }

    if (patient.getId() != null) {
      report.setId(patient.getIdElement().getIdPart());
    }

    report.setExcluded(excluded);

    return report;
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

  public static Practitioner toPractitioner(DecodedJWT jwt) {
    Practitioner practitioner = new Practitioner();
    practitioner.getMeta().addTag(Constants.MainSystem, Constants.LinkUserTag, null);
    List identifiers = new ArrayList();
    Identifier identifier = new Identifier();
    identifier.setSystem(Constants.MainSystem);
    identifier.setValue(jwt.getSubject());
    identifiers.add(identifier);
    practitioner.setIdentifier(identifiers);
    String payload = jwt.getPayload();
    byte[] decodedBytes = Base64.getDecoder().decode(payload);
    String decodedString = new String(decodedBytes);
    JsonObject jsonObject = new JsonParser().parse(decodedString).getAsJsonObject();
    List<HumanName> list = new ArrayList<>();
    HumanName dst = new HumanName();
    if (jsonObject.has("family_name")) {
      dst.setFamily(jsonObject.get("family_name").toString());
    }
    if (jsonObject.has("given_name")) {
      List<StringType> givenNames = new ArrayList();
      givenNames.add(new StringType(jsonObject.get("given_name").toString()));
      dst.setGiven(givenNames);
    }
    list.add(dst);
    practitioner.setName(list);
    if (jsonObject.has("email")) {
      List<ContactPoint> contactPointList = new ArrayList();
      ContactPoint email = new ContactPoint();
      email.setSystem(ContactPoint.ContactPointSystem.EMAIL);
      email.setValue(jsonObject.get("email").toString());
      contactPointList.add(email);
      practitioner.setTelecom(contactPointList);
    }
    return practitioner;
  }


  public static Bundle getBundle(String content) {
    FhirContext ctx = FhirContext.forR4();
    IParser jsonParser = ctx.newJsonParser();
    IParser xmlParser = ctx.newXmlParser();
    Bundle reportDefBundle;
    try {
      if (content.trim().startsWith("{") || content.trim().startsWith("[")) {
        reportDefBundle = jsonParser.parseResource(Bundle.class, content);
      } else {
        reportDefBundle = xmlParser.parseResource(Bundle.class, content);
      }
    } catch (Exception ex) {
      logger.error(String.format("Error parsing report def bundle due to %s", ex.getMessage()));
      return null;
    }
    return reportDefBundle;
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
