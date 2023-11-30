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
        return encounter.getSubject().hasReference() && encounter.getSubject().getReference().equals("Patient/" + patientId);
      case Observation:
        Observation observation = (Observation) resource;
        return observation.getSubject().hasReference() && observation.getSubject().getReference().equals("Patient/" + patientId);
      case MedicationRequest:
        MedicationRequest medicationRequest = (MedicationRequest) resource;
        return medicationRequest.getSubject().hasReference() && medicationRequest.getSubject().getReference().equals("Patient/" + patientId);
      case MedicationAdministration:
        MedicationAdministration medicationAdministration = (MedicationAdministration) resource;
        return medicationAdministration.getSubject().hasReference() && medicationAdministration.getSubject().getReference().equals("Patient/" + patientId);
      case MedicationDispense:
        MedicationDispense medicationDispense = (MedicationDispense) resource;
        return medicationDispense.getSubject().hasReference() && medicationDispense.getSubject().getReference().equals("Patient/" + patientId);
      case MedicationStatement:
        MedicationStatement medicationStatement = (MedicationStatement) resource;
        return medicationStatement.getSubject().hasReference() && medicationStatement.getSubject().getReference().equals("Patient/" + patientId);
      case Condition:
        Condition condition = (Condition) resource;
        return condition.getSubject().hasReference() && condition.getSubject().getReference().equals("Patient/" + patientId);
      case Procedure:
        Procedure procedure = (Procedure) resource;
        return procedure.getSubject().getReference().equals("Patient/" + patientId);
      case Immunization:
        Immunization immunization = (Immunization) resource;
        return immunization.getPatient().hasReference() && immunization.getPatient().getReference().equals("Patient/" + patientId);
      case DiagnosticReport:
        DiagnosticReport diagnosticReport = (DiagnosticReport) resource;
        return diagnosticReport.getSubject().hasReference() && diagnosticReport.getSubject().getReference().equals("Patient/" + patientId);
      case DocumentReference:
        DocumentReference documentReference = (DocumentReference) resource;
        return documentReference.getSubject().hasReference() && documentReference.getSubject().getReference().equals("Patient/" + patientId);
      case List:
        ListResource listResource = (ListResource) resource;
        return listResource.getSubject().hasReference() && listResource.getSubject().getReference().equals("Patient/" + patientId);
      case MeasureReport:
        MeasureReport measureReport = (MeasureReport) resource;
        return measureReport.getSubject().hasReference() && measureReport.getSubject().getReference().equals("Patient/" + patientId);
      case RiskAssessment:
        RiskAssessment riskAssessment = (RiskAssessment) resource;
        return riskAssessment.getSubject().hasReference() && riskAssessment.getSubject().getReference().equals("Patient/" + patientId);
      case CarePlan:
        CarePlan carePlan = (CarePlan) resource;
        return carePlan.getSubject().hasReference() && carePlan.getSubject().getReference().equals("Patient/" + patientId);
      case Goal:
        Goal goal = (Goal) resource;
        return goal.getSubject().hasReference() && goal.getSubject().getReference().equals("Patient/" + patientId);
      case ServiceRequest:
        ServiceRequest serviceRequest = (ServiceRequest) resource;
        return serviceRequest.getSubject().hasReference() && serviceRequest.getSubject().getReference().equals("Patient/" + patientId);
      case Communication:
        Communication communication = (Communication) resource;
        return communication.getSubject().hasReference() && communication.getSubject().getReference().equals("Patient/" + patientId);
      case CommunicationRequest:
        CommunicationRequest communicationRequest = (CommunicationRequest) resource;
        return communicationRequest.getSubject().hasReference() && communicationRequest.getSubject().getReference().equals("Patient/" + patientId);
      case DeviceRequest:
        DeviceRequest deviceRequest = (DeviceRequest) resource;
        return deviceRequest.getSubject().hasReference() && deviceRequest.getSubject().getReference().equals("Patient/" + patientId);
      case DeviceUseStatement:
        DeviceUseStatement deviceUseStatement = (DeviceUseStatement) resource;
        return deviceUseStatement.getSubject().hasReference() && deviceUseStatement.getSubject().getReference().equals("Patient/" + patientId);
      case Flag:
        Flag flag = (Flag) resource;
        return flag.getSubject().hasReference() && flag.getSubject().getReference().equals("Patient/" + patientId);
      case FamilyMemberHistory:
        FamilyMemberHistory familyMemberHistory = (FamilyMemberHistory) resource;
        return familyMemberHistory.getPatient().hasReference() && familyMemberHistory.getPatient().getReference().equals("Patient/" + patientId);
      case ClinicalImpression:
        ClinicalImpression clinicalImpression = (ClinicalImpression) resource;
        return clinicalImpression.getSubject().hasReference() && clinicalImpression.getSubject().getReference().equals("Patient/" + patientId);
      case Consent:
        Consent consent = (Consent) resource;
        return consent.getPatient().hasReference() && consent.getPatient().getReference().equals("Patient/" + patientId);
      case DetectedIssue:
        DetectedIssue detectedIssue = (DetectedIssue) resource;
        return detectedIssue.getPatient().hasReference() && detectedIssue.getPatient().getReference().equals("Patient/" + patientId);
      case NutritionOrder:
        NutritionOrder nutritionOrder = (NutritionOrder) resource;
        return nutritionOrder.getPatient().hasReference() && nutritionOrder.getPatient().getReference().equals("Patient/" + patientId);
      case Specimen:
        Specimen specimen = (Specimen) resource;
        return specimen.getSubject().hasReference() && specimen.getSubject().getReference().equals("Patient/" + patientId);
      case BodyStructure:
        BodyStructure bodyStructure = (BodyStructure) resource;
        return bodyStructure.getPatient().hasReference() && bodyStructure.getPatient().getReference().equals("Patient/" + patientId);
      case ImagingStudy:
        ImagingStudy imagingStudy = (ImagingStudy) resource;
        return imagingStudy.getSubject().hasReference() && imagingStudy.getSubject().getReference().equals("Patient/" + patientId);
      case Media:
        Media media = (Media) resource;
        return media.getSubject().hasReference() && media.getSubject().getReference().equals("Patient/" + patientId);
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
