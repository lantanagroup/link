package com.lantanagroup.link;

import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class FhirBundler {
  protected static final Logger logger = LoggerFactory.getLogger(FhirBundler.class);

  @Setter
  private FhirDataProvider fhirDataProvider;

  public FhirBundler(FhirDataProvider fhirDataProvider) {
    this.fhirDataProvider = fhirDataProvider;
  }

  private List<DomainResource> getPatientResources(MeasureReport patientMeasureReport) {
    String patientId = patientMeasureReport.getSubject().getReference().replace("Patient/", "");

    // Find all references to contained supplemental data resources
    List<String> patientDataReferences = patientMeasureReport.getExtension().stream()
            .filter(ext -> {
              if (StringUtils.isEmpty(ext.getUrl()) || !ext.getUrl().equals(Constants.ExtensionSupplementalData)) {
                return false;
              }

              if (!(ext.getValue() instanceof Reference)) {
                return false;
              }

              Reference ref = (Reference) ext.getValue();
              return ref.hasReference() && ref.getReference().startsWith("#");
            })
            .map(ext -> {
              return ((Reference) ext.getValue()).getReference();
            })
            .collect(Collectors.toList());

    // Return a list of all the resources identified as supplemental data
    List<DomainResource> patientResources = new ArrayList<>();

    for (String patientDataReference : patientDataReferences) {
      Optional<Resource> found = patientMeasureReport.getContained().stream()
              .filter(r -> r.getIdElement().equals(patientDataReference))
              .findFirst();
      if (found.isPresent()) {
        patientResources.add(FhirHelper.cleanResource((DomainResource) found.get()));
      } else {
        logger.error(String.format("Could not find resource %s", patientDataReference));
      }
    }

    //If the bundle doesn't already have the patient resource, add it in
    if(patientResources.stream().noneMatch(i -> i.getResourceType().toString().equals("Patient") && i.getIdElement().getIdPart().equals(patientId))) {
      List<DomainResource> patient = patientMeasureReport.getContained().stream()
              .filter(r -> r.getResourceType().toString().equals("Patient") && r.getIdElement().getIdPart().equals(patientId))
              .map(r -> (DomainResource) r).collect(Collectors.toList());

      //Even if there's more than one copy of the Patient resource that exists in the Bundle for some reason, only add the first
      if (patient.size() > 0) {
        patientResources.add(FhirHelper.cleanResource(patient.get(0)));
      }
    }

    return patientResources;
  }

  /**
   * Generates a bundle of resources based on the master measure report. Gets all individual measure
   * reports from the master measure report, then gets all the evaluatedResources from the individual
   * measure reports, and bundles them all together.
   * @param sendWholeBundle
   * @param removeContainedResources
   * @param masterMeasureReports
   * @return
   */
  public Bundle generateBundle(
          boolean sendWholeBundle,
          boolean removeContainedResources,
          List<MeasureReport> masterMeasureReports,
          DocumentReference documentReference) {
    Meta meta = new Meta();
    meta.addProfile(Constants.MeasureReportBundleProfileUrl);
    meta.addTag(Constants.MainSystem, "report", "Report");

    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.COLLECTION);
    bundle.setTimestamp(new Date());
    bundle.setMeta(meta);

    bundle.getIdentifier()
            .setSystem(Constants.IdentifierSystem)
            .setValue(UUID.randomUUID().toString());

    // Add census list(s) to the report bundle
    if (documentReference != null) {
      List<ListResource> censusLists = FhirHelper.getCensusLists(documentReference, this.fhirDataProvider);
      bundle.getEntry().addAll(censusLists.stream().map(censusList -> {
        Bundle.BundleEntryComponent newCensusListEntry = new Bundle.BundleEntryComponent();
        newCensusListEntry.setResource(censusList);
        return newCensusListEntry;
      }).collect(Collectors.toList()));
    }

    for (MeasureReport masterMeasureReport : masterMeasureReports) {

      // Add the master measure report to the bundle
      bundle.addEntry().setResource(FhirHelper.cleanResource(masterMeasureReport));

      // If configured to include patient resources...
      if (sendWholeBundle) {
        // Get the references to the individual patient measure reports from the master
        List<String> patientMeasureReportReferences = FhirHelper.getPatientMeasureReportReferences(masterMeasureReport);

        // Retrieve the individual patient measure reports from the server
        List<MeasureReport> patientReports = FhirHelper.getPatientReports(patientMeasureReportReferences, this.fhirDataProvider);

        for (MeasureReport patientMeasureReport : patientReports) {
          if (patientMeasureReport == null) {
            continue;
          }

          MeasureReport clonedPatientMeasureReport = (MeasureReport) FhirHelper.cleanResource(patientMeasureReport);

          if (removeContainedResources) {
            clonedPatientMeasureReport.getContained().clear();
          }

          // Add the individual patient measure report to the bundle
          bundle.addEntry().setResource(clonedPatientMeasureReport);

          // Get all the supplemental data resources in the individual patient measure reports and add them to the bundle
          List<DomainResource> patientResources = this.getPatientResources(patientMeasureReport);

          for (DomainResource patientResource : patientResources) {
            if (bundle.getEntry().stream()
                    .map(Bundle.BundleEntryComponent::getResource)
                    .anyMatch(r -> r.getIdElement().equals(patientResource.getIdElement()))) {
              continue;
            }
            bundle.addEntry().setResource(patientResource);
          }
        }
      }
    }

    if(bundle.getEntry() != null) {
      for (Bundle.BundleEntryComponent e : bundle.getEntry()) {
        e.setFullUrl(String.format(
                "http://nhsnlink.org/fhir/%s/%s",
                e.getResource().getResourceType(),
                e.getResource().getIdElement().getIdPart().replaceAll("^#", "")));
      }
    }

    // Bundle is complete
    return bundle;
  }
}
