package com.lantanagroup.link;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.EncodingEnum;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.config.api.ApiReportDefsUrlConfig;
import com.lantanagroup.link.db.model.Report;
import com.lantanagroup.link.serialize.FhirJsonDeserializer;
import com.lantanagroup.link.serialize.FhirJsonSerializer;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


public class FhirHelper {
  private static final Logger logger = LoggerFactory.getLogger(FhirHelper.class);

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

  public static <T extends IBaseResource> T parseResource(Class<T> resourceType, String string) {
    return EncodingEnum.detectEncoding(string)
            .newParser(FhirContextProvider.getFhirContext())
            .parseResource(resourceType, string);
  }

  /**
   * Creates a bundle of the same type as the measure definition bundle and another bundle of type batch,
   * Then traverses the measure definition bundle, adding all the ValueSet and CodeSystem resources to the
   * batch bundle and stores them on the configured TX server while all the other resources are added to the
   * new measure definition bundle to be returned.
   *
   * @param bundle measure definition bundle
   * @param config contains Api configurations
   * @return A bundle of the same type as the measureDefBundle bundle without any of the ValueSet or CodeSystem resources.
   */
  public static Bundle storeTerminologyAndReturnOther(Bundle bundle, ApiConfig config) {
    if (bundle.hasEntry()) {
      FhirDataProvider fhirDataProvider = new FhirDataProvider(config.getTerminologyService());
      Bundle txBundle = new Bundle();
      Bundle returnBundle = new Bundle();
      txBundle.setType(Bundle.BundleType.BATCH);
      returnBundle.setType(bundle.getType());
      for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
        entry.getRequest().setMethod(Bundle.HTTPVerb.PUT);
        if (entry.getResource() instanceof ValueSet || entry.getResource() instanceof CodeSystem) {
          txBundle.addEntry(entry);
        } else {
          returnBundle.addEntry(entry);
        }
      }
      if (txBundle.hasEntry()) {
        fhirDataProvider.transaction(txBundle);
      }
      return returnBundle;
    }
    return bundle;
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

  public static List<ListResource> getPatientLists(Report report) {
    return report.getPatientLists().stream().map(pl -> {
      ListResource listResource = new ListResource();
      listResource.setId(pl.getId());

      listResource.setEntry(pl.getPatients().stream().map(pid -> {
        ListResource.ListEntryComponent entry = new ListResource.ListEntryComponent();

        if (StringUtils.isNotEmpty(pid.getIdentifier())) {
          String[] identifierSplit = pid.getIdentifier().split("|");
          Identifier identifier = new Identifier();
          entry.getItem().setIdentifier(identifier);

          if (identifierSplit.length == 2) {
            identifier.setSystem(identifierSplit[0]);
            identifier.setValue(identifierSplit[1]);
          } else if (identifierSplit.length == 1) {
            identifier.setValue(identifierSplit[0]);
          } else {
            logger.error("Expected one or two parts to the identifier, but got {}", identifierSplit.length);
          }
        } else if (StringUtils.isNotEmpty(pid.getReference())) {
          entry.getItem().setReference(pid.getReference());
        }

        return entry;
      }).collect(Collectors.toList()));

      return listResource;
    }).collect(Collectors.toList());
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
    Optional<ApiReportDefsUrlConfig> measureReportAggregatorUrl = config.getReportDefs().getUrls().stream().filter(urlConfig -> {
      String bundleId = urlConfig.getBundleId();
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
}

