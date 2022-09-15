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

  private List<DomainResource> getPatientResources(String reportId, MeasureReport patientMeasureReport) {
    String patientId = patientMeasureReport.getSubject().getReference().replace("Patient/", "");
    Bundle patientDataBundle = (Bundle) this.fhirDataProvider.getResourceByTypeAndId("Bundle", reportId + "-" + patientId.hashCode());

    if (patientDataBundle == null) {
      logger.error(String.format("Did not find patient data bundle Bundle/%s-%s", reportId, patientId.hashCode()));
      return new ArrayList<>();
    }

    // Add all references to evaluated resources to the bundle as entries with a request method & url
    List<String> patientDataReferences = patientMeasureReport.getExtension().stream()
            .filter(ext -> {
              if (StringUtils.isEmpty(ext.getUrl()) || !ext.getUrl().equals(Constants.ExtensionSupplementalData)) {
                return false;
              }

              if (!(ext.getValue() instanceof Reference)) {
                return false;
              }

              Reference ref = (Reference) ext.getValue();

              // Evaluated resources that don't have the populationReference extension did fully satisfy
              // the criteria of at least one CQL definition
              if (ref.getExtensionsByUrl(Constants.ExtensionCriteriaReference).isEmpty()) {
                return false;
              }

              // TODO: Reverse this and ONLY include contained resources so that we aren't sending EHR data as-is from the source.
              if (StringUtils.isEmpty(ref.getReference()) || ref.getReference().startsWith("#")) {
                return false;
              }

              return true;
            })
            .map(ext -> {
              return ((Reference) ext.getValue()).getReference();
            })
            .collect(Collectors.toList());

    // Return a list of all the resources identified by evaluated resources
    List<DomainResource> patientResources = new ArrayList<>();

    for (String patientDataReference : patientDataReferences) {
      Optional<Bundle.BundleEntryComponent> found = patientDataBundle.getEntry().stream().filter(e -> {
        String thisReference = e.getResource().getResourceType().toString() + "/" + e.getResource().getIdElement().getIdPart();
        return thisReference.equals(patientDataReference);
      }).findFirst();

      if (found.isPresent()) {
        patientResources.add(FhirHelper.cleanResource((DomainResource) found.get().getResource()));
      } else {
        logger.error(String.format("Could not find resource %s for in bundle %s", patientDataReference, patientDataBundle.getId()));
      }
    }

    //If the bundle doesn't already have the patient resource, add it in
    if(patientResources.stream().noneMatch(i -> i.getResourceType().toString().equals("Patient") && i.getIdElement().getIdPart().equals(patientId))) {
      List<DomainResource> patient = patientDataBundle.getEntry().stream()
              .filter(e -> e.getResource().getResourceType().toString().equals("Patient") && e.getResource().getIdElement().getIdPart().equals(patientId))
              .map(e -> (DomainResource) e.getResource()).collect(Collectors.toList());

      //Even if there's more than one copy of the Patient resource that exists in the Bundle for some reason, only add the first
      if (patient.size() > 0) {
        patientResources.add(FhirHelper.cleanResource(patient.get(0)));
      }
    }

    return patientResources;
  }

  /**
   * Removes any MeasureReport.evaluatedResource entries that represent a contained resource, and removes
   * the associated contained resource from the MeasureReport. These contained Observations are FHIR Measure SDE's
   * that are not of interest to the recipient of the reports
   * @param report
   */
  private void removeContainedEvaluatedResource(MeasureReport report) {
    List<Reference> containedEvaluatedResources = report.getEvaluatedResource().stream()
            .filter(er -> er.getReference() != null && er.getReference().startsWith("#"))
            .collect(Collectors.toList());

    containedEvaluatedResources.forEach(cer -> {
      Optional<Resource> contained = report.getContained().stream()
              .filter(c -> c.getIdElement() != null && c.getIdElement().getIdPart().replace("#", "").equals(cer.getReference().substring(1)) && c.getResourceType() == ResourceType.Observation)
              .findFirst();

      if (contained.isPresent()) {
        report.getContained().remove(contained.get());
        report.getEvaluatedResource().remove(cer);
      }
    });
  }

  /**
   * Generates a bundle of resources based on the master measure report. Gets all individual measure
   * reports from the master measure report, then gets all the evaluatedResources from the individual
   * measure reports, and bundles them all together.
   * @param includePopulationSubjectResults
   * @param removeContainedEvaluatedResources
   * @param masterMeasureReport
   * @return
   */
  public Bundle generateBundle(
          boolean includePopulationSubjectResults,
          boolean removeContainedEvaluatedResources,
          MeasureReport masterMeasureReport,
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

    // Add the master measure report to the bundle
    bundle.addEntry().setResource(FhirHelper.cleanResource(masterMeasureReport));

    // Add census list(s) to the report bundle
    if (documentReference != null) {
      List<ListResource> censusLists = FhirHelper.getCensusLists(documentReference, this.fhirDataProvider);
      bundle.getEntry().addAll(censusLists.stream().map(censusList -> {
        Bundle.BundleEntryComponent newCensusListEntry = new Bundle.BundleEntryComponent();
        newCensusListEntry.setResource(censusList);
        return newCensusListEntry;
      }).collect(Collectors.toList()));
    }

    // If configured to include all resources...
    if (includePopulationSubjectResults) {
      // Get the references to the individual patient measure reports from the master
      List<String> patientMeasureReportReferences = FhirHelper.getPatientMeasureReportReferences(masterMeasureReport);

      // Retrieve the individual patient measure reports from the server
      List<MeasureReport> patientReports = FhirHelper.getPatientReports(patientMeasureReportReferences, this.fhirDataProvider);

      for (MeasureReport patientMeasureReport : patientReports) {
        if (patientMeasureReport == null) {
          continue;
        }

        MeasureReport clonedPatientMeasureReport = (MeasureReport) FhirHelper.cleanResource(patientMeasureReport);

        if (removeContainedEvaluatedResources) {
          this.removeContainedEvaluatedResource(clonedPatientMeasureReport);
        }

        // Add the individual patient measure report to the bundle
        bundle.addEntry().setResource(clonedPatientMeasureReport);

        // Get all the evaluated resources in the individual patient measure reports and add them to the bundle
        List<DomainResource> patientResources = this.getPatientResources(documentReference.getMasterIdentifier().getValue(), patientMeasureReport);

        for (DomainResource patientResource : patientResources) {
          bundle.addEntry().setResource(patientResource);
        }
      }
    }

    if(bundle.getEntry() != null) {
      for (Bundle.BundleEntryComponent r : bundle.getEntry()) {
        r.setFullUrl("http://nhsnlink.org/fhir/"
                + r.getResource().getResourceType() + "/"
                + r.getResource().getIdElement().getIdPart());
      }
    }

    // Bundle is complete
    return bundle;
  }
}
