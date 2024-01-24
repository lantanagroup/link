package com.lantanagroup.link;

import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class FhirBundlerEntrySorter {
  private static final Logger logger = LoggerFactory.getLogger(FhirBundlerEntrySorter.class);

  private static List<String> getPatientIds(Bundle bundle) {
    List<String> patientIds = new ArrayList<>();
    for (Bundle.BundleEntryComponent e : bundle.getEntry()) {
      if (e.getResource().getResourceType().equals(ResourceType.Patient)) {
        patientIds.add(e.getResource().getIdElement().getIdPart());
      }
    }
    return patientIds.stream().distinct().collect(Collectors.toList());
  }

  private static Bundle.BundleEntryComponent getLinkOrganization(Bundle bundle) {
    return bundle.getEntry().stream()
            .filter(e -> {
              return e.getResource().getResourceType().equals(ResourceType.Organization) &&
                      e.getResource().getMeta().hasProfile(Constants.SubmittingOrganizationProfile);
            })
            .findFirst()
            .orElse(null);
  }

  private static Bundle.BundleEntryComponent getLinkDevice(Bundle bundle) {
    return bundle.getEntry().stream()
            .filter(e -> {
              return e.getResource().getResourceType().equals(ResourceType.Device) &&
                      e.getResource().getMeta().hasProfile(Constants.SubmittingDeviceProfile);
            })
            .findFirst()
            .orElse(null);
  }

  private static Bundle.BundleEntryComponent getLinkQueryPlanLibrary(Bundle bundle) {
    return bundle.getEntry().stream()
            .filter(e -> e.getResource().getResourceType().equals(ResourceType.Library))
            .filter(e -> {
              Library library = (Library) e.getResource();
              return library.getType().getCoding().stream()
                      .anyMatch(c -> c.getCode().equals(Constants.LibraryTypeModelDefinitionCode));
            })
            .findFirst()
            .orElse(null);
  }

  private static List<Bundle.BundleEntryComponent> getLinkCensusList(Bundle bundle) {
    return bundle.getEntry().stream()
            .filter(e ->
                    e.getResource().getResourceType().equals(ResourceType.List) &&
                            e.getResource().getMeta().getProfile().stream()
                                    .anyMatch(p -> p.getValue().equals(Constants.CensusProfileUrl)))
            .sorted(new ResourceComparator())
            .collect(Collectors.toList());
  }

  public static void sort(Bundle bundle) {
    logger.info("Sorting bundle");
    List<Bundle.BundleEntryComponent> newEntriesList = new ArrayList<>();
    HashMap<String, List<Bundle.BundleEntryComponent>> patientResources = new HashMap<>();

    for (Bundle.BundleEntryComponent e : bundle.getEntry()) {
      String patientReference = getPatientReference(e.getResource());
      if (patientReference != null) {
        String patientId = patientReference.replace("Patient/", "");
        patientResources.computeIfAbsent(patientId, key -> new ArrayList<>()).add(e);
      }
    }

    List<String> patientIds = getPatientIds(bundle);
    Bundle.BundleEntryComponent organization = getLinkOrganization(bundle);
    Bundle.BundleEntryComponent device = getLinkDevice(bundle);
    List<Bundle.BundleEntryComponent> censusLists = getLinkCensusList(bundle);
    Bundle.BundleEntryComponent queryPlanLibrary = getLinkQueryPlanLibrary(bundle);

    // Link Organization is first
    if (organization != null) {
      logger.info("Adding organization");
      newEntriesList.add(organization);
    }

    // Link Device are next
    if (device != null) {
      logger.info("Adding device");
      newEntriesList.add(device);
    }

    // Link Census List is next
    if (!censusLists.isEmpty()) {
      logger.info("Adding census");
      censusLists.stream()
              .sorted(Comparator.comparing(e -> Objects.requireNonNullElse(getMeasureName(e), "")))
              .forEachOrdered(newEntriesList::add);
    }

    // Link Query Plan is next
    if (queryPlanLibrary != null) {
      logger.info("Adding query plan");
      newEntriesList.add(queryPlanLibrary);
    }

    logger.info("Adding aggregate measure reports");
    bundle.getEntry().stream()
            .filter(e -> e.getResource().getResourceType().equals(ResourceType.MeasureReport) && ((MeasureReport) e.getResource()).getType().equals(MeasureReport.MeasureReportType.SUBJECTLIST))
            .sorted(new ResourceComparator())
            .forEach(newEntriesList::add);

    // Loop through each patient and add the patients resources in the following order:
    // MeasureReport, Patient, All other resources sorted by resourceType/id
    for (String patientId : patientIds) {
      logger.debug("Adding patient resources: {}", patientId);
      List<Bundle.BundleEntryComponent> relatedPatientResources = patientResources.get(patientId);
      Bundle.BundleEntryComponent indMeasureReport = relatedPatientResources.stream()
              .filter(r -> r.getResource().getResourceType().equals(ResourceType.MeasureReport))
              .findFirst()
              .orElse(null);
      Bundle.BundleEntryComponent patient = relatedPatientResources.stream()
              .filter(r -> r.getResource().getResourceType().equals(ResourceType.Patient))
              .findFirst()
              .orElse(null);

      if (indMeasureReport == null || patient == null) {
        logger.warn("Patient {} is missing a MeasureReport or Patient resource", patientId);
        continue;
      }

      // Individual MeasureReport is next
      newEntriesList.add(indMeasureReport);

      // Patient is next
      newEntriesList.add(patient);

      // All other resources are next, sorted by resourceType/id
      relatedPatientResources.stream()
              .filter(r -> !r.getResource().getResourceType().equals(ResourceType.MeasureReport) && !r.getResource().getResourceType().equals(ResourceType.Patient))
              .sorted(new ResourceComparator())
              .forEach(newEntriesList::add);
    }

    // Get all resources not already in the bundle
    logger.info("Adding remaining resources");
    HashSet<String> newEntryReferences = new HashSet<>();
    for (Bundle.BundleEntryComponent e : newEntriesList) {
      newEntryReferences.add(e.getResource().getResourceType().toString() + "/" + e.getResource().getIdElement().getIdPart());
    }

    bundle.getEntry().stream()
            .filter(r -> !newEntryReferences.contains(r.getResource().getResourceType().toString() + "/" + r.getResource().getIdElement().getIdPart()))
            .sorted(new ResourceComparator())
            .forEach(newEntriesList::add);

    // Clear the bundle entries and add the sorted entries
    logger.info("Replacing entries");
    bundle.getEntry().clear();
    bundle.getEntry().addAll(newEntriesList);

    logger.info("Done sorting bundle");
  }

  /**
   * Returns patient's reference of the related resource
   *
   * @param resource The resource to check
   * @return The patient's reference of the related resource, or null if resource or subject is not related to a patient or is null
   */
  private static String getPatientReference(Resource resource) {
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
    }

    return null;
  }

  private static String getMeasureName(Bundle.BundleEntryComponent censusList) {
    Resource resource = censusList.getResource();
    if (!(resource instanceof ListResource)) {
      return null;
    }
    ListResource list = (ListResource) resource;
    if (!list.hasIdentifier()) {
      return null;
    }
    return list.getIdentifierFirstRep().getValue();
  }

  static class ResourceComparator implements Comparator<Bundle.BundleEntryComponent> {
    @Override
    public int compare(Bundle.BundleEntryComponent r1, Bundle.BundleEntryComponent r2) {
      String r1Reference = r1.getResource().getResourceType().toString() + "/" + r1.getResource().getIdElement().getIdPart();
      String r2Reference = r2.getResource().getResourceType().toString() + "/" + r2.getResource().getIdElement().getIdPart();
      return r1Reference.compareTo(r2Reference);
    }
  }
}
