package com.lantanagroup.link;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.util.BundleUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.gson.*;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.config.api.ApiReportDefsBundleConfig;
import com.lantanagroup.link.model.PatientReportModel;
import com.lantanagroup.link.serialize.FhirJsonDeserializer;
import com.lantanagroup.link.serialize.FhirJsonSerializer;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;


public class FhirHelper {
  private static final Logger logger = LoggerFactory.getLogger(FhirHelper.class);
  private static final String NAME = "name";
  private static final String SUBJECT = "sub";
  private static final String DOCUMENT_REFERENCE_VERSION_URL = "https://www.cdc.gov/nhsn/fhir/nhsnlink/StructureDefinition/nhsnlink-report-version";

  public static void recordAuditEvent(HttpServletRequest request, FhirDataProvider fhirDataProvider, DecodedJWT jwt, AuditEventTypes type, String outcomeDescription) {
    AuditEvent auditEvent = createAuditEvent(request, jwt, type, outcomeDescription);

    try {
      MethodOutcome outcome = fhirDataProvider.createOutcome(auditEvent);
      IIdType id = outcome.getId();
      logger.info("AuditEvent LOGGED: " + id.getValue());
    } catch (Exception ex) {
      logger.error("Failed to record AuditEvent", ex);
    }
  }

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
        break;
      case SearchReports:
        auditEvent.setType(new Coding(null, "search-reports", null));
        break;
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

    JsonObject jsonObject = JsonParser.parseString(decodedString).getAsJsonObject();
    if (jsonObject.has(NAME)) {
      agent.setName(jsonObject.get(NAME).toString());
    }
    if (jsonObject.has(SUBJECT)) {
      agent.setAltId(jsonObject.get(SUBJECT).toString());
    }

    String remoteAddress;
    remoteAddress = getRemoteAddress(request);

    if (remoteAddress != null) {
      agent.setNetwork(new AuditEvent.AuditEventAgentNetworkComponent().setAddress(remoteAddress));
    }

    if (jsonObject.has("aud") && !jsonObject.get("aud").isJsonNull()) {
      logger.info(String.format("Aud is : " + jsonObject.get("aud").toString()));
      JsonElement aud = jsonObject.get("aud");
      String identifierValue = "";
      if (aud instanceof JsonPrimitive) {
        identifierValue = aud.getAsString();
      } else if (aud instanceof JsonArray) {
        for (int i = 0; i < ((JsonArray) aud).size(); i++) {
          identifierValue += ((JsonArray) aud).get(i).getAsString();
        }
      }
      Identifier identifier = new Identifier().setValue(identifierValue);
      agent.setLocation(new Reference().setIdentifier(identifier));
    }
    agentList.add(agent);
    auditEvent.setAgent(agentList);

    return auditEvent;
  }

  public static String getRemoteAddress(HttpServletRequest request) {
    String remoteAddress;
    if (request.getHeader("X-FORWARED-FOR") != null) {
      logger.debug("X-FORWARED-FOR IP is: " + request.getHeader("X-FORWARED-FOR"));
    }

    if (request.getHeader("X-REAL-IP") != null) {
      logger.debug("X-REAL-IP is: " + request.getHeader("X-REAL-IP") + " and is being used for remoteAddress");
      remoteAddress = request.getHeader("X-REAL-IP");
    } else {
      logger.debug("X-REAL-IP IP is not found.");
      remoteAddress = request.getRemoteAddr() != null ? (request.getRemoteHost() != null ? request.getRemoteAddr() + "(" + request.getRemoteHost() + ")" : request.getRemoteAddr()) : "";
    }
    return remoteAddress;
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

  public static <T extends IBaseResource> List<T> getAllPages(Bundle bundle, FhirDataProvider fhirDataProvider, FhirContext ctx, Class<T> resourceType) {
    List<T> resources = new ArrayList<>(BundleUtil.toListOfResourcesOfType(ctx, bundle, resourceType));

    // Load the subsequent pages
    while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
      bundle = fhirDataProvider
              .getClient()
              .loadPage()
              .next(bundle)
              .execute();
      logger.info("Adding next page of resources...");
      resources.addAll(BundleUtil.toListOfResourcesOfType(ctx, bundle, resourceType));
    }
    return resources;
  }

  public static List<IBaseResource> getAllPages(Bundle bundle, FhirDataProvider fhirDataProvider, FhirContext ctx) {
    return getAllPages(bundle, fhirDataProvider, ctx, IBaseResource.class);
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
    List<Identifier> identifiers = new ArrayList<>();
    Identifier identifier = new Identifier();
    identifier.setSystem(Constants.MainSystem);
    identifier.setValue(jwt.getSubject());
    identifiers.add(identifier);
    practitioner.setIdentifier(identifiers);
    String payload = jwt.getPayload();
    byte[] decodedBytes = Base64.getDecoder().decode(payload);
    String decodedString = new String(decodedBytes);
    JsonObject jsonObject = JsonParser.parseString(decodedString).getAsJsonObject();
    List<HumanName> list = new ArrayList<>();
    HumanName dst = new HumanName();
    if (jsonObject.has("family_name")) {
      dst.setFamily(jsonObject.get("family_name").toString());
    }
    if (jsonObject.has("given_name")) {
      ArrayList<StringType> givenNames = new ArrayList<>();
      givenNames.add(new StringType(jsonObject.get("given_name").toString()));
      dst.setGiven(givenNames);
    }
    list.add(dst);
    practitioner.setName(list);
    if (jsonObject.has("email")) {
      ArrayList<ContactPoint> contactPointList = new ArrayList<>();
      ContactPoint email = new ContactPoint();
      email.setSystem(ContactPoint.ContactPointSystem.EMAIL);
      email.setValue(jsonObject.get("email").toString());
      contactPointList.add(email);
      practitioner.setTelecom(contactPointList);
    }
    return practitioner;
  }

  public static <T extends IBaseResource> T parseResource(Class<T> resourceType, String string) {
    return EncodingEnum.detectEncoding(string)
            .newParser(FhirContextProvider.getFhirContext())
            .parseResource(resourceType, string);
  }

  /**
   * Creates a Bundle of type "batch" with an entry for each resource provided. Expects that each resource
   * have an id, so that it can create a "request" with a method of "PUT resourceType/id"
   * Example: createUpdateBatch(List.of(resource1, resource2, resource3))
   *
   * @param resources The list of resources to be added to the batch
   * @return Bundle that can be executed as a transaction on the FHIR server
   */
  public static Bundle createUpdateBatch(List<DomainResource> resources) {
    Bundle newBundle = new Bundle();
    newBundle.setType(Bundle.BundleType.BATCH);

    for (DomainResource resource : resources) {
      Bundle.BundleEntryComponent newEntry = new Bundle.BundleEntryComponent();
      newEntry
              .setResource(resource)
              .getRequest()
              .setMethod(Bundle.HTTPVerb.PUT)
              .setUrl(resource.getResourceType().toString() + "/" + resource.getIdElement().getIdPart());
    }

    return newBundle;
  }

  /**
   * Traverse each population in the measure report and find the subject results of the population,
   * which is a reference to a contained List resource. The contained List resource contains each of the
   * individual patient measure reports that is used to calculate the aggregate value of the population.
   *
   * @param masterMeasureReport The master measure report to search for lists of individual reports
   * @return The list of unique references to individual patient MeasureReport resources that comprise the master
   */
  public static List<String> getPatientMeasureReportReferences(MeasureReport masterMeasureReport) {
    List<String> references = new ArrayList<>();

    // Loop through the groups and populations within each group
    // Look for a reference to a contained List resource representing the measure reports
    // that comprise the group/population's aggregate
    for (MeasureReport.MeasureReportGroupComponent group : masterMeasureReport.getGroup()) {
      for (MeasureReport.MeasureReportGroupPopulationComponent population : group.getPopulation()) {
        String populateListRef = population.getSubjectResults().getReference();
        Optional<ListResource> populationList = masterMeasureReport
                .getContained().stream()
                .filter(c -> c.getIdElement().getIdPart().equals(populateListRef))
                .map(c -> (ListResource) c)
                .findFirst();

        // If a contained List resource was found, extract each MeasureReport reference from the list
        if (populationList.isPresent()) {
          for (ListResource.ListEntryComponent listEntry : populationList.get().getEntry()) {
            String individualReportRef = listEntry.getItem().getReference();

            // Should only be references to MeasureReport. Skip if not.
            if (!individualReportRef.startsWith("MeasureReport/")) {
              continue;
            }

            // Only add the references to the list of it is not already in the list (create a unique list of MR references)
            if (!references.contains(listEntry.getItem().getReference())) {
              references.add(listEntry.getItem().getReference());
            }
          }
        }
      }
    }

    return references;
  }

  public static List<MeasureReport> getPatientReports(List<String> patientMeasureReportReferences, FhirDataProvider fhirDataProvider) {
    Bundle patientReportsReqBundle = new Bundle();
    patientReportsReqBundle.setType(Bundle.BundleType.TRANSACTION);

    for (String patientMeasureReportReference : patientMeasureReportReferences) {
      patientReportsReqBundle.addEntry().getRequest()
              .setMethod(Bundle.HTTPVerb.GET)
              .setUrl(patientMeasureReportReference);
    }

    // Get each of the individual patient measure reports
    return fhirDataProvider.transaction(patientReportsReqBundle)
            .getEntry().stream()
            .map(e -> (MeasureReport) e.getResource())
            .collect(Collectors.toList());
  }

  public static Set<String> getDataRequirementTypes(Bundle reportDefBundle) {
    return reportDefBundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> resource instanceof Library)
            .flatMap(resource -> ((Library) resource).getDataRequirement().stream().map(DataRequirement::getType))
            .collect(Collectors.toSet());
  }

  public static boolean validLibraries(Bundle reportRefBundle) throws Exception {

    Measure measure = FhirHelper.getMeasure(reportRefBundle);
    List<String> measureIncludedLibraries = measure.getLibrary().stream().map(canonUrl -> canonUrl.getValue()).collect(Collectors.toList());

    // get only the "primary" libraries from the bundle - the ones that have the url included in the Measure resource under "Library" section
    List<Library> libraryList = reportRefBundle.getEntry().stream()
            .filter(e -> e.getResource() instanceof Library && measureIncludedLibraries.contains(((Library) e.getResource()).getUrl()))
            .map(e -> (Library) e.getResource()).collect(Collectors.toList());

    List<Library> libraryEmptyList = libraryList.stream().filter(library -> library.getDataRequirement().isEmpty()).collect(Collectors.toList());
    return libraryEmptyList.isEmpty();
  }

  public static List<String> getQueryConfigurationDataReqMissingResourceTypes(List<String> properties, Bundle measureDefBundle) {
    // get data requirements
    Set<String> reportDefBundleDataReqSet = getDataRequirementTypes(measureDefBundle);
    // get all resources types that are in data requirements but missing from query properties
    return reportDefBundleDataReqSet.stream().filter(e -> !e.equals("Patient") && !properties.contains(e)).collect(Collectors.toList());
  }

  public static List<String> getQueryConfigurationDataReqCommonResourceTypes(List<String> properties, Bundle measureDefBundle) {
    // get data requirements
    Set<String> reportDefBundleDataReqSet = getDataRequirementTypes(measureDefBundle);
    // get all resources types that are in data requirements but missing from query properties
    return reportDefBundleDataReqSet.stream().filter(properties::contains).collect(Collectors.toList());
  }

  public static List<ListResource> getCensusLists(DocumentReference documentReference, FhirDataProvider fhirDataProvider) {
    if (documentReference != null && documentReference.getContext() != null && documentReference.getContext().hasRelated()) {
      Bundle requestBundle = new Bundle();
      requestBundle.setType(Bundle.BundleType.TRANSACTION);

      // Add each census list reference to the batch bundle as a GET
      requestBundle.getEntry().addAll(documentReference.getContext().getRelated().stream().map(related -> {
        Bundle.BundleEntryComponent newEntry = new Bundle.BundleEntryComponent();
        newEntry.getRequest()
                .setMethod(Bundle.HTTPVerb.GET)
                .setUrl(related.getReference());
        return newEntry;
      }).collect(Collectors.toList()));

      // Execute the batch/transaction to retrieve each census list
      Bundle responseBundle = fhirDataProvider.transaction(requestBundle);

      // Return a list of the census retrieved as part of the batch/transaction
      return responseBundle.getEntry().stream()
              .map(entry -> (ListResource) entry.getResource())
              .collect(Collectors.toList());
    } else {
      logger.warn("The DocumentReference does not have a context/related census associated with it");
    }

    return new ArrayList<>();
  }

  public static String getSubmittedLocation(DocumentReference documentReference) {
    String bundleLocation = "";
    for (DocumentReference.DocumentReferenceContentComponent content : documentReference.getContent()) {
      if (content.hasAttachment() && content.getAttachment().hasUrl()) {
        bundleLocation = content.getAttachment().getUrl();
      }
    }
    return bundleLocation;
  }

  // TODO: This typically creates a second content element, which I don't think we want
  //       The one initially created in ReportGenerator.generateDocumentReference has a creation date but no URL
  public static void setSubmissionLocation(DocumentReference documentReference, String location) {
    documentReference.getContent().removeIf(c -> c.hasAttachment() && c.getAttachment().hasUrl());
    DocumentReference.DocumentReferenceContentComponent newContent = new DocumentReference.DocumentReferenceContentComponent();
    newContent.getAttachment().setUrl(location);
    documentReference.getContent().add(newContent);
  }

  public static void initSerializers(SimpleModule module, IParser jsonParser) {
    List.of("Account", "ActivityDefinition", "AdverseEvent", "AllergyIntolerance", "Appointment", "AppointmentResponse", "AuditEvent", "Basic", "Binary", "BiologicallyDerivedProduct", "BodyStructure", "Bundle", "CapabilityStatement", "CarePlan", "CareTeam", "CatalogEntry", "ChargeItem", "ChargeItemDefinition", "Claim", "ClaimResponse", "ClinicalImpression", "CodeSystem", "Communication", "CommunicationRequest", "CompartmentDefinition", "Composition", "ConceptMap", "Condition", "Consent", "Contract", "Coverage", "CoverageEligibilityRequest", "CoverageEligibilityResponse", "DetectedIssue", "Device", "DeviceDefinition", "DeviceMetric", "DeviceRequest", "DeviceUseStatement", "DiagnosticReport", "DocumentManifest", "DocumentReference", "EffectEvidenceSynthesis", "Encounter", "Endpoint", "EnrollmentRequest", "EnrollmentResponse", "EpisodeOfCare", "EventDefinition", "Evidence", "EvidenceVariable", "ExampleScenario", "ExplanationOfBenefit", "FamilyMemberHistory", "Flag", "Goal", "GraphDefinition", "Group", "GuidanceResponse", "HealthcareService", "ImagingStudy", "Immunization", "ImmunizationEvaluation", "ImmunizationRecommendation", "ImplementationGuide", "InsurancePlan", "Invoice", "Library", "Linkage", "ListResource", "Location", "Measure", "MeasureReport", "Media", "Medication", "MedicationAdministration", "MedicationDispense", "MedicationKnowledge", "MedicationRequest", "MedicationStatement", "MedicinalProduct", "MedicinalProductAuthorization", "MedicinalProductContraindication", "MedicinalProductIndication", "MedicinalProductIngredient", "MedicinalProductInteraction", "MedicinalProductManufactured", "MedicinalProductPackaged", "MedicinalProductPharmaceutical", "MedicinalProductUndesirableEffect", "MessageDefinition", "MessageHeader", "MolecularSequence", "NamingSystem", "NutritionOrder", "Observation", "ObservationDefinition", "OperationDefinition", "OperationOutcome", "Organization", "OrganizationAffiliation", "Parameters", "Patient", "PaymentNotice", "PaymentReconciliation", "Person", "PlanDefinition", "Practitioner", "PractitionerRole", "Procedure", "Provenance", "Questionnaire", "QuestionnaireResponse", "RelatedPerson", "RequestGroup", "ResearchDefinition", "ResearchElementDefinition", "ResearchStudy", "ResearchSubject", "RiskAssessment", "RiskEvidenceSynthesis", "Schedule", "SearchParameter", "ServiceRequest", "Slot", "Specimen", "SpecimenDefinition", "StructureDefinition", "StructureMap", "Subscription", "Substance", "SubstancePolymer", "SubstanceProtein", "SubstanceReferenceInformation", "SubstanceSpecification", "SubstanceSourceMaterial", "SupplyDelivery", "SupplyRequest", "Task", "TerminologyCapabilities", "TestReport", "TestScript", "ValueSet", "VerificationResult", "VisionPrescription")
            .forEach(e -> {
              try {
                Class<? extends IBaseResource> theClass = Class.forName("org.hl7.fhir.r4.model." + e).asSubclass(IBaseResource.class);
                module.addSerializer(new FhirJsonSerializer<>(jsonParser, theClass));
                module.addDeserializer(theClass, new FhirJsonDeserializer<>(jsonParser));
              } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
              }
            });
  }

  /**
   * Reads the configuration file and figures out what aggregator to instantiate for a given measure bundle
   *
   * @param reportDefBundle
   * @param config
   * @return Returns the aggregator class for that measure
   */
  public static String getReportAggregatorClassName(ApiConfig config, Bundle reportDefBundle) {
    String reportAggregatorClassName = null;

    Optional<ApiReportDefsBundleConfig> measureReportAggregatorUrl = config.getReportDefs().getBundles().stream().filter(bundleConfig -> {
      String bundleId = bundleConfig.getBundleId();
      return bundleId.equalsIgnoreCase(reportDefBundle.getIdElement().getIdPart());
    }).findFirst();

    if (measureReportAggregatorUrl.isPresent() && !StringUtils.isEmpty(measureReportAggregatorUrl.get().getReportAggregator())) {
      reportAggregatorClassName = measureReportAggregatorUrl.get().getReportAggregator();
    } else {
      reportAggregatorClassName = config.getReportAggregator();
    }
    logger.info(String.format("Using aggregator %s for measure %s", reportAggregatorClassName, reportDefBundle.getId()));
    return reportAggregatorClassName;
  }


  public static Measure getMeasure(Bundle reportDefBundle) throws Exception {
    return reportDefBundle.getEntry().stream()
            .filter(entry -> entry.getResource() instanceof Measure)
            .map(entry -> (Measure) entry.getResource())
            .findFirst()
            .orElseThrow(() -> new Exception("Report def does not contain a measure"));
  }

  /**
   * Copies entries from {@code list2} into {@code list1} that are not already present in {@code list1}.
   * Entries are considered equal if their items' references or identifiers are equal.
   */
  public static void mergeCensusLists(ListResource list1, ListResource list2) {
    for (ListResource.ListEntryComponent entry2 : list2.getEntry()) {
      Reference item2 = entry2.getItem();
      boolean exists = list1.getEntry().stream().anyMatch(entry1 -> {
        Reference item1 = entry1.getItem();
        if (item1.hasReference() && StringUtils.equals(item1.getReference(), item2.getReference())) {
          return true;
        }
        if (item1.hasIdentifier() && item1.getIdentifier().equalsShallow(item2.getIdentifier())) {
          return true;
        }
        return false;
      });
      if (!exists) {
        list1.addEntry(entry2.copy());
      }
    }
  }


  public enum AuditEventTypes {
    Generate,
    ExcludePatients,
    Export,
    Send,
    SearchLocations,
    InitiateQuery,
    SearchReports,
    Transformation
  }
}

