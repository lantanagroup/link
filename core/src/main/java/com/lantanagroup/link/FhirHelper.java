package com.lantanagroup.link;

import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Strings;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.ConceptMap;
import com.lantanagroup.link.db.model.Report;
import com.lantanagroup.link.db.model.tenant.Address;
import com.lantanagroup.link.model.ApiVersionModel;
import com.lantanagroup.link.serialize.FhirJsonDeserializer;
import com.lantanagroup.link.serialize.FhirJsonSerializer;
import com.lantanagroup.link.validation.ClasspathBasedValidationSupport;
import com.lantanagroup.link.validation.Validator;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class FhirHelper {
  private static final Logger logger = LoggerFactory.getLogger(FhirHelper.class);

  private static final DefaultProfileValidationSupport validationSupport =
          new DefaultProfileValidationSupport(FhirContextProvider.getFhirContext());

  static {
    validationSupport.fetchAllStructureDefinitions();
  }

  public static org.hl7.fhir.r4.model.Address getFHIRAddress(Address address) {
    org.hl7.fhir.r4.model.Address ret = new org.hl7.fhir.r4.model.Address();

    if (!Strings.isNullOrEmpty(address.getAddressLine())) {
      ret.getLine().add(new StringType(address.getAddressLine()));
    }

    if (!Strings.isNullOrEmpty(address.getCity())) {
      ret.setCity(address.getCity());
    }

    if (!Strings.isNullOrEmpty(address.getState())) {
      ret.setState(address.getState());
    }

    if (!Strings.isNullOrEmpty(address.getPostalCode())) {
      ret.setPostalCode(address.getPostalCode());
    }

    if (!Strings.isNullOrEmpty(address.getCountry())) {
      ret.setCountry(address.getCountry());
    }

    return ret;
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

  public static CodeableConcept createCodeableConcept(String code, String system){
    CodeableConcept type = new CodeableConcept();
    List<Coding> codings = new ArrayList<>();
    Coding coding = new Coding();

    coding.setCode(code);
    coding.setSystem(system);

    codings.add(coding);

    type.setCoding(codings);

    return type;
  }

  /**
   * @param report
   */
  public static void incrementMajorVersion(Report report) {
    if (StringUtils.isEmpty(report.getVersion())) {
      report.setVersion("1.0");
    } else {
      String version = report.getVersion();
      version = version.substring(0, version.indexOf("."));
      report.setVersion((Integer.parseInt(version) + 1) + ".0");
    }
  }

  public static Set<String> getDataRequirementTypes(Bundle reportDefBundle) {
    return reportDefBundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> resource instanceof Library)
            .flatMap(resource -> ((Library) resource).getDataRequirement().stream().map(DataRequirement::getType))
            .collect(Collectors.toSet());
  }

  public static List<Library> getMainLibraries(Bundle reportDefBundle) throws Exception {
    Measure measure = FhirHelper.getMeasure(reportDefBundle);
    List<String> measureIncludedLibraries = measure.getLibrary().stream().map(canonUrl -> canonUrl.getValue()).collect(Collectors.toList());

    // get only the "primary" libraries from the bundle - the ones that have the url included in the Measure resource under "Library" section
    return reportDefBundle.getEntry().stream()
            .filter(e -> e.getResource() instanceof Library && measureIncludedLibraries.contains(((Library) e.getResource()).getUrl()))
            .map(e -> (Library) e.getResource()).collect(Collectors.toList());
  }

  public static boolean validLibraries(Bundle reportDefBundle) throws Exception {
    return getMainLibraries(reportDefBundle).stream()
            .noneMatch(library -> library.getDataRequirement().isEmpty());
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
  public static void mergePatientLists(ListResource list1, ListResource list2) {
    for (ListResource.ListEntryComponent entry2 : list2.getEntry()) {
      Reference item2 = entry2.getItem();
      boolean exists = list1.getEntry().stream().anyMatch(entry1 -> {
        Reference item1 = entry1.getItem();
        if (item1.hasReference() && StringUtils.equals(item1.getReference(), item2.getReference())) {
          return true;
        }
        return item1.hasIdentifier() && item1.getIdentifier().equalsShallow(item2.getIdentifier());
      });
      if (!exists) {
        list1.addEntry(entry2.copy());
      }
    }
  }

  public static String toString(Base value) {
    return toString(value, true);
  }

  public static String toString(Base value, boolean includeType) {
    if (value instanceof PrimitiveType<?>) {
      PrimitiveType<?> primitive = (PrimitiveType<?>) value;
      return StringUtils.truncate(primitive.asStringValue(), 100);
    }
    return String.format(
            "%s{%s}",
            includeType ? value.getClass().getSimpleName() : "",
            value.children().stream()
                    .filter(property -> !property.getValues().isEmpty())
                    .map(FhirHelper::toString)
                    .collect(Collectors.joining(";")));
  }

  public static String toString(Property property) {
    return String.format(
            "%s=[%s]",
            property.getName(),
            property.getValues().stream()
                    .map(value -> toString(value, false))
                    .collect(Collectors.joining(",")));
  }

  public static <T extends Base> void walk(Base ancestor, Class<T> resourceType, Consumer<T> consumer) {
    if (resourceType.isInstance(ancestor)) {
      consumer.accept(resourceType.cast(ancestor));
    }
    for (Property property : ancestor.children()) {
      for (Base child : property.getValues()) {
        walk(child, resourceType, consumer);
      }
    }
  }

  public static <T extends Base> List<T> collect(Base ancestor, Class<T> resourceType) {
    List<T> resources = new ArrayList<>();
    walk(ancestor, resourceType, resources::add);
    return resources;
  }

  public static boolean hasNonzeroPopulationCount(MeasureReport measureReport) {
    return measureReport.getGroup().stream()
            .flatMap(group -> group.getPopulation().stream())
            .mapToInt(MeasureReport.MeasureReportGroupPopulationComponent::getCount)
            .anyMatch(count -> count > 0);
  }

  public static Device getDevice(ApiConfig apiConfig) {
    ApiVersionModel apiVersionModel = Helper.getVersionInfo();
    Device device = new Device();
    device.setId(UUID.randomUUID().toString());
    device.getMeta().addProfile(Constants.SubmittingDeviceProfile);
    device.addDeviceName().setName(apiConfig.getName());
    device.getDeviceNameFirstRep().setType(Device.DeviceNameType.USERFRIENDLYNAME);

    if (StringUtils.isNotEmpty(apiVersionModel.getVersion())) {
      device.addVersion()
              .setType(new CodeableConcept().addCoding(new Coding().setCode("version")))
              .setComponent(new Identifier().setValue("api"))
              .setValue(apiVersionModel.getVersion());
    }

    if (StringUtils.isNotEmpty(apiVersionModel.getBuild())) {
      device.addVersion()
              .setType(new CodeableConcept().addCoding(new Coding().setCode("build").setSystem(Constants.LinkDeviceVersionCodeSystem)))
              .setComponent(new Identifier().setValue("api"))
              .setValue(apiVersionModel.getBuild());
    }

    if (StringUtils.isNotEmpty(apiVersionModel.getCommit())) {
      device.addVersion()
              .setType(new CodeableConcept().addCoding(new Coding().setCode("commit").setSystem(Constants.LinkDeviceVersionCodeSystem)))
              .setComponent(new Identifier().setValue("api"))
              .setValue(apiVersionModel.getCommit());
    }

    return device;
  }

  public static Device getDevice(ApiConfig apiConfig, TenantService tenantService, Validator validator) {
    Device device = getDevice(apiConfig);

    if (tenantService.getConfig().getEvents() != null) {
      addEventNotesToDevice(device, "BeforeMeasureResolution", tenantService.getConfig().getEvents().getBeforeMeasureResolution());
      addEventNotesToDevice(device, "AfterMeasureResolution", tenantService.getConfig().getEvents().getAfterMeasureResolution());
      addEventNotesToDevice(device, "OnRegeneration", tenantService.getConfig().getEvents().getOnRegeneration());
      addEventNotesToDevice(device, "BeforePatientOfInterestLookup", tenantService.getConfig().getEvents().getBeforePatientOfInterestLookup());
      addEventNotesToDevice(device, "AfterPatientOfInterestLookup", tenantService.getConfig().getEvents().getAfterPatientOfInterestLookup());
      addEventNotesToDevice(device, "BeforePatientDataQuery", tenantService.getConfig().getEvents().getBeforePatientDataQuery());
      addEventNotesToDevice(device, "AfterPatientResourceQuery", tenantService.getConfig().getEvents().getAfterPatientResourceQuery());
      addEventNotesToDevice(device, "AfterPatientDataQuery", tenantService.getConfig().getEvents().getAfterPatientDataQuery());
      addEventNotesToDevice(device, "AfterApplyConceptMaps", tenantService.getConfig().getEvents().getAfterApplyConceptMaps());
      addEventNotesToDevice(device, "BeforePatientDataStore", tenantService.getConfig().getEvents().getBeforePatientDataStore());
      addEventNotesToDevice(device, "AfterPatientDataStore", tenantService.getConfig().getEvents().getAfterPatientDataStore());
      addEventNotesToDevice(device, "BeforeMeasureEval", tenantService.getConfig().getEvents().getBeforeMeasureEval());
      addEventNotesToDevice(device, "AfterMeasureEval", tenantService.getConfig().getEvents().getAfterMeasureEval());
      addEventNotesToDevice(device, "BeforeReportStore", tenantService.getConfig().getEvents().getBeforeReportStore());
      addEventNotesToDevice(device, "AfterReportStore", tenantService.getConfig().getEvents().getAfterReportStore());
      addEventNotesToDevice(device, "BeforeBundling", tenantService.getConfig().getEvents().getBeforeBundling());
      addEventNotesToDevice(device, "AfterBundling", tenantService.getConfig().getEvents().getAfterBundling());
    }

    List<com.lantanagroup.link.db.model.ConceptMap> conceptMaps = tenantService.getAllConceptMaps();

    if (!conceptMaps.isEmpty()) {
      for (ConceptMap conceptMap : conceptMaps) {
        String title = conceptMap.getConceptMap().getTitle();
        String version = StringUtils.isNotEmpty(conceptMap.getConceptMap().getVersion()) ? " (" + conceptMap.getConceptMap().getVersion() + ")" : "";
        if (StringUtils.isNotEmpty(title)) {
          title = conceptMap.getConceptMap().getName();
        } else {
          title = conceptMap.getId();
        }

        device.getNote().add(new Annotation().setText("Concept Map: " + title + version));
      }
    }

    setImplementationGuideNotes(device, validator);

    return device;
  }

  public static void setImplementationGuideNotes(Device device, Validator validator) {
    String prefix = "Implementation Guide: ";
    device.getNote().removeIf(annotation -> annotation.getText().startsWith(prefix));

    for (ImplementationGuide ig : ClasspathBasedValidationSupport.getInstance().getImplementationGuides()) {
      String igNote = String.format("%s%s - %s - %s - %s", prefix, StringUtils.isEmpty(ig.getTitle()) ? ig.getName() : ig.getTitle(), ig.getUrl(), ig.getVersion(), ig.getDate());
      device.addNote(new Annotation().setText(igNote));
    }
  }

  private static void addEventNotesToDevice(Device device, String eventCategory, List<String> events) {
    if (events == null) {
      return;
    }

    for (String event : events) {
      device.getNote().add(new Annotation().setText("Event: " + eventCategory + " executes " + event));
    }
  }

  public static FHIRPathEngine getFhirPathEngine() {
    HapiWorkerContext workerContext = new HapiWorkerContext(FhirContextProvider.getFhirContext(), validationSupport);
    return new FHIRPathEngine(workerContext);
  }

  /**
   * Returns patient's reference of the related resource
   *
   * @param resource The resource to check
   * @return The patient's reference of the related resource, or null if resource or subject is not related to a patient or is null
   */
  public static String getPatientReference(Resource resource) {
    switch (resource.getResourceType()) {
      case Patient:
        return resource.getIdElement().getIdPart();
      case Encounter:
        Encounter encounter = (Encounter) resource;
        return (encounter.getSubject() != null) ? encounter.getSubject().getReference() : null;
      case Observation:
        Observation observation = (Observation) resource;
        return (observation.getSubject() != null) ? observation.getSubject().getReference() : null;
      case MedicationRequest:
        MedicationRequest medicationRequest = (MedicationRequest) resource;
        return (medicationRequest.getSubject() != null) ? medicationRequest.getSubject().getReference() : null;
      case MedicationAdministration:
        MedicationAdministration medicationAdministration = (MedicationAdministration) resource;
        return (medicationAdministration.getSubject() != null) ? medicationAdministration.getSubject().getReference() : null;
      case MedicationDispense:
        MedicationDispense medicationDispense = (MedicationDispense) resource;
        return (medicationDispense.getSubject() != null) ? medicationDispense.getSubject().getReference() : null;
      case MedicationStatement:
        MedicationStatement medicationStatement = (MedicationStatement) resource;
        return (medicationStatement.getSubject() != null) ? medicationStatement.getSubject().getReference() : null;
      case Condition:
        Condition condition = (Condition) resource;
        return (condition.getSubject() != null) ? condition.getSubject().getReference() : null;
      case Procedure:
        Procedure procedure = (Procedure) resource;
        return (procedure.getSubject() != null) ? procedure.getSubject().getReference() : null;
      case Immunization:
        Immunization immunization = (Immunization) resource;
        return (immunization.getPatient() != null) ? immunization.getPatient().getReference() : null;
      case DiagnosticReport:
        DiagnosticReport diagnosticReport = (DiagnosticReport) resource;
        return (diagnosticReport.getSubject() != null) ? diagnosticReport.getSubject().getReference() : null;
      case DocumentReference:
        DocumentReference documentReference = (DocumentReference) resource;
        return (documentReference.getSubject() != null) ? documentReference.getSubject().getReference() : null;
      case List:
        ListResource listResource = (ListResource) resource;
        return (listResource.getSubject() != null) ? listResource.getSubject().getReference() : null;
      case MeasureReport:
        MeasureReport measureReport = (MeasureReport) resource;
        return (measureReport.getSubject() != null) ? measureReport.getSubject().getReference() : null;
      case RiskAssessment:
        RiskAssessment riskAssessment = (RiskAssessment) resource;
        return (riskAssessment.getSubject() != null) ? riskAssessment.getSubject().getReference() : null;
      case CarePlan:
        CarePlan carePlan = (CarePlan) resource;
        return (carePlan.getSubject() != null) ? carePlan.getSubject().getReference() : null;
      case Goal:
        Goal goal = (Goal) resource;
        return (goal.getSubject() != null) ? goal.getSubject().getReference() : null;
      case ServiceRequest:
        ServiceRequest serviceRequest = (ServiceRequest) resource;
        return (serviceRequest.getSubject() != null) ? serviceRequest.getSubject().getReference() : null;
      case Communication:
        Communication communication = (Communication) resource;
        return (communication.getSubject() != null) ? communication.getSubject().getReference() : null;
      case CommunicationRequest:
        CommunicationRequest communicationRequest = (CommunicationRequest) resource;
        return (communicationRequest.getSubject() != null) ? communicationRequest.getSubject().getReference() : null;
      case DeviceRequest:
        DeviceRequest deviceRequest = (DeviceRequest) resource;
        return (deviceRequest.getSubject() != null) ? deviceRequest.getSubject().getReference() : null;
      case DeviceUseStatement:
        DeviceUseStatement deviceUseStatement = (DeviceUseStatement) resource;
        return (deviceUseStatement.getSubject() != null) ? deviceUseStatement.getSubject().getReference() : null;
      case Flag:
        Flag flag = (Flag) resource;
        return (flag.getSubject() != null) ? flag.getSubject().getReference() : null;
      case FamilyMemberHistory:
        FamilyMemberHistory familyMemberHistory = (FamilyMemberHistory) resource;
        return (familyMemberHistory.getPatient() != null) ? familyMemberHistory.getPatient().getReference() : null;
      case ClinicalImpression:
        ClinicalImpression clinicalImpression = (ClinicalImpression) resource;
        return (clinicalImpression.getSubject() != null) ? clinicalImpression.getSubject().getReference() : null;
      case Consent:
        Consent consent = (Consent) resource;
        return (consent.getPatient() != null) ? consent.getPatient().getReference() : null;
      case DetectedIssue:
        DetectedIssue detectedIssue = (DetectedIssue) resource;
        return (detectedIssue.getPatient() != null) ? detectedIssue.getPatient().getReference() : null;
      case NutritionOrder:
        NutritionOrder nutritionOrder = (NutritionOrder) resource;
        return (nutritionOrder.getPatient() != null) ? nutritionOrder.getPatient().getReference() : null;
      case Specimen:
        Specimen specimen = (Specimen) resource;
        return (specimen.getSubject() != null) ? specimen.getSubject().getReference() : null;
      case BodyStructure:
        BodyStructure bodyStructure = (BodyStructure) resource;
        return (bodyStructure.getPatient() != null) ? bodyStructure.getPatient().getReference() : null;
      case ImagingStudy:
        ImagingStudy imagingStudy = (ImagingStudy) resource;
        return (imagingStudy.getSubject() != null) ? imagingStudy.getSubject().getReference() : null;
      case Media:
        Media media = (Media) resource;
        return (media.getSubject() != null) ? media.getSubject().getReference() : null;
      case Coverage:
        Coverage coverage = (Coverage) resource;
        return (coverage.getBeneficiary() != null) ? coverage.getBeneficiary().getReference() : null;
    }

    return null;
  }
}
