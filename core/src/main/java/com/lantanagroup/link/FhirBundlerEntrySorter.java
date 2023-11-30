package com.lantanagroup.link;

import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FhirBundlerEntrySorter {
  private static final Logger logger = LoggerFactory.getLogger(FhirBundlerEntrySorter.class);

  private static List<String> getPatientIds(Bundle bundle) {
    return bundle.getEntry().stream()
            .filter(e -> e.getResource().getResourceType().equals(ResourceType.Patient))
            .map(e -> e.getResource().getIdElement().getIdPart())
            .collect(Collectors.toList());
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

  private static Bundle.BundleEntryComponent getLinkCensusList(Bundle bundle) {
    return bundle.getEntry().stream()
            .filter(e ->
                    e.getResource().getResourceType().equals(ResourceType.List) &&
                            e.getResource().getMeta().getProfile().stream()
                                    .anyMatch(p -> p.getValue().equals(Constants.CensusProfileUrl)))
            .findFirst()
            .orElse(null);
  }

  private static List<Bundle.BundleEntryComponent> getRelatedPatientResources(Bundle bundle, String patientId) {
    return bundle.getEntry().stream()
            .filter(e -> isResourceRelatedToPatient(e.getResource(), patientId))
            .collect(Collectors.toList());
  }

  public static void sort(Bundle bundle) {
    List<Bundle.BundleEntryComponent> newEntriesList = new ArrayList<>();

    List<String> patientIds = getPatientIds(bundle);
    Bundle.BundleEntryComponent organization = getLinkOrganization(bundle);
    Bundle.BundleEntryComponent device = getLinkDevice(bundle);
    Bundle.BundleEntryComponent censusList = getLinkCensusList(bundle);
    Bundle.BundleEntryComponent queryPlanLibrary = getLinkQueryPlanLibrary(bundle);

    // Link Organization is first
    if (organization != null) {
      newEntriesList.add(organization);
    }

    // Link Device are next
    if (device != null) {
      newEntriesList.add(device);
    }

    // Link Census List is next
    if (censusList != null) {
      newEntriesList.add(censusList);
    }

    // Link DocumentReference is next
    if (queryPlanLibrary != null) {
      newEntriesList.add(queryPlanLibrary);
    }

    List<Bundle.BundleEntryComponent> aggregateMeasureReports = bundle.getEntry().stream()
            .filter(e -> e.getResource().getResourceType().equals(ResourceType.MeasureReport) && ((MeasureReport) e.getResource()).getType().equals(MeasureReport.MeasureReportType.SUBJECTLIST))
            .sorted(new ResourceComparator())
            .collect(Collectors.toList());
    newEntriesList.addAll(aggregateMeasureReports);

    // Loop through each patient and add the patients resources in the following order:
    // MeasureReport, Patient, All other resources sorted by resourceType/id
    for (String patientId : patientIds) {
      List<Bundle.BundleEntryComponent> relatedPatientResources = getRelatedPatientResources(bundle, patientId);
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
      List<Bundle.BundleEntryComponent> otherPatientResources = relatedPatientResources.stream()
              .filter(r -> !r.getResource().getResourceType().equals(ResourceType.MeasureReport) && !r.getResource().getResourceType().equals(ResourceType.Patient))
              .sorted(new ResourceComparator())
              .collect(Collectors.toList());
      newEntriesList.addAll(otherPatientResources);
    }

    // Get all resources not already in the bundle
    List<Bundle.BundleEntryComponent> otherNonPatientResources = bundle.getEntry().stream()
            .filter(r -> !newEntriesList.contains(r))
            .sorted(new ResourceComparator())
            .collect(Collectors.toList());
    newEntriesList.addAll(otherNonPatientResources);

    // Clear the bundle entries and add the sorted entries
    bundle.getEntry().clear();
    bundle.getEntry().addAll(newEntriesList);
  }

  private static boolean isReferenceToPatient(Reference reference, String patientId) {
    return reference.hasReference() && reference.getReference().equals("Patient/" + patientId);
  }

  /**
   * Returns true if the resource is related to the patient
   *
   * @param resource  The resource to check
   * @param patientId The patient id
   * @return True if the resource is related to the patient
   */
  private static boolean isResourceRelatedToPatient(Resource resource, String patientId) {
    switch (resource.getResourceType()) {
      case Patient:
        return resource.getIdElement().getIdPart().equals(patientId);
      case Encounter:
        Encounter encounter = (Encounter) resource;
        return isReferenceToPatient(encounter.getSubject(), patientId);
      case Observation:
        Observation observation = (Observation) resource;
        return isReferenceToPatient(observation.getSubject(), patientId);
      case MedicationRequest:
        MedicationRequest medicationRequest = (MedicationRequest) resource;
        return isReferenceToPatient(medicationRequest.getSubject(), patientId);
      case MedicationAdministration:
        MedicationAdministration medicationAdministration = (MedicationAdministration) resource;
        return isReferenceToPatient(medicationAdministration.getSubject(), patientId);
      case MedicationDispense:
        MedicationDispense medicationDispense = (MedicationDispense) resource;
        return isReferenceToPatient(medicationDispense.getSubject(), patientId);
      case MedicationStatement:
        MedicationStatement medicationStatement = (MedicationStatement) resource;
        return isReferenceToPatient(medicationStatement.getSubject(), patientId);
      case Condition:
        Condition condition = (Condition) resource;
        return isReferenceToPatient(condition.getSubject(), patientId);
      case Procedure:
        Procedure procedure = (Procedure) resource;
        return isReferenceToPatient(procedure.getSubject(), patientId);
      case Immunization:
        Immunization immunization = (Immunization) resource;
        return isReferenceToPatient(immunization.getPatient(), patientId);
      case DiagnosticReport:
        DiagnosticReport diagnosticReport = (DiagnosticReport) resource;
        return isReferenceToPatient(diagnosticReport.getSubject(), patientId);
      case DocumentReference:
        DocumentReference documentReference = (DocumentReference) resource;
        return isReferenceToPatient(documentReference.getSubject(), patientId);
      case List:
        ListResource listResource = (ListResource) resource;
        return isReferenceToPatient(listResource.getSubject(), patientId);
      case MeasureReport:
        MeasureReport measureReport = (MeasureReport) resource;
        return isReferenceToPatient(measureReport.getSubject(), patientId);
      case RiskAssessment:
        RiskAssessment riskAssessment = (RiskAssessment) resource;
        return isReferenceToPatient(riskAssessment.getSubject(), patientId);
      case CarePlan:
        CarePlan carePlan = (CarePlan) resource;
        return isReferenceToPatient(carePlan.getSubject(), patientId);
      case Goal:
        Goal goal = (Goal) resource;
        return isReferenceToPatient(goal.getSubject(), patientId);
      case ServiceRequest:
        ServiceRequest serviceRequest = (ServiceRequest) resource;
        return isReferenceToPatient(serviceRequest.getSubject(), patientId);
      case Communication:
        Communication communication = (Communication) resource;
        return isReferenceToPatient(communication.getSubject(), patientId);
      case CommunicationRequest:
        CommunicationRequest communicationRequest = (CommunicationRequest) resource;
        return isReferenceToPatient(communicationRequest.getSubject(), patientId);
      case DeviceRequest:
        DeviceRequest deviceRequest = (DeviceRequest) resource;
        return isReferenceToPatient(deviceRequest.getSubject(), patientId);
      case DeviceUseStatement:
        DeviceUseStatement deviceUseStatement = (DeviceUseStatement) resource;
        return isReferenceToPatient(deviceUseStatement.getSubject(), patientId);
      case Flag:
        Flag flag = (Flag) resource;
        return isReferenceToPatient(flag.getSubject(), patientId);
      case FamilyMemberHistory:
        FamilyMemberHistory familyMemberHistory = (FamilyMemberHistory) resource;
        return isReferenceToPatient(familyMemberHistory.getPatient(), patientId);
      case ClinicalImpression:
        ClinicalImpression clinicalImpression = (ClinicalImpression) resource;
        return isReferenceToPatient(clinicalImpression.getSubject(), patientId);
      case Consent:
        Consent consent = (Consent) resource;
        return isReferenceToPatient(consent.getPatient(), patientId);
      case DetectedIssue:
        DetectedIssue detectedIssue = (DetectedIssue) resource;
        return isReferenceToPatient(detectedIssue.getPatient(), patientId);
      case NutritionOrder:
        NutritionOrder nutritionOrder = (NutritionOrder) resource;
        return isReferenceToPatient(nutritionOrder.getPatient(), patientId);
      case Specimen:
        Specimen specimen = (Specimen) resource;
        return isReferenceToPatient(specimen.getSubject(), patientId);
      case BodyStructure:
        BodyStructure bodyStructure = (BodyStructure) resource;
        return isReferenceToPatient(bodyStructure.getPatient(), patientId);
      case ImagingStudy:
        ImagingStudy imagingStudy = (ImagingStudy) resource;
        return isReferenceToPatient(imagingStudy.getSubject(), patientId);
      case Media:
        Media media = (Media) resource;
        return isReferenceToPatient(media.getSubject(), patientId);
    }

    return false;
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
