package com.lantanagroup.link;

import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Strings;
import com.lantanagroup.link.db.model.Report;
import com.lantanagroup.link.db.model.tenant.Address;
import com.lantanagroup.link.serialize.FhirJsonDeserializer;
import com.lantanagroup.link.serialize.FhirJsonSerializer;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class FhirHelper {
  private static final Logger logger = LoggerFactory.getLogger(FhirHelper.class);

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
}
