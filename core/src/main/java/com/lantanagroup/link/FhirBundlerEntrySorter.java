package com.lantanagroup.link;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class FhirBundlerEntrySorter {
  private static final Logger logger = LoggerFactory.getLogger(FhirBundlerEntrySorter.class);

  public static void sort(Bundle bundle) {
    logger.info("Sorting bundle");
    FhirBundleProcessor fhirBundleProcessor = new FhirBundleProcessor(bundle);
    List<Bundle.BundleEntryComponent> newEntriesList = new ArrayList<>();

    HashMap<String, List<Bundle.BundleEntryComponent>> patientResources = fhirBundleProcessor.getPatientResources();
    Bundle.BundleEntryComponent organization = fhirBundleProcessor.getLinkOrganization();
    Bundle.BundleEntryComponent device = fhirBundleProcessor.getLinkDevice();
    List<Bundle.BundleEntryComponent> censusLists = fhirBundleProcessor.getLinkCensusLists();
    Bundle.BundleEntryComponent queryPlanLibrary = fhirBundleProcessor.getLinkQueryPlanLibrary();

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
      newEntriesList.addAll(censusLists);
    }

    // Link DocumentReference is next
    if (queryPlanLibrary != null) {
      logger.info("Adding query plan");
      newEntriesList.add(queryPlanLibrary);
    }

    logger.info("Adding aggregate measure reports");
    newEntriesList.addAll(fhirBundleProcessor.getAggregateMeasureReports());

    // Loop through each patient and add the patients resources in the following order:
    // MeasureReport, Patient, All other resources sorted by resourceType/id
    for (String patientId : fhirBundleProcessor.getPatientIds()) {
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
    fhirBundleProcessor.getOtherResources().stream()
            .sorted(new ResourceComparator())
            .forEach(newEntriesList::add);

    // Clear the bundle entries and add the sorted entries
    logger.info("Replacing entries");
    bundle.getEntry().clear();
    bundle.getEntry().addAll(newEntriesList);

    logger.info("Done sorting bundle");
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
