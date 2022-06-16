package com.lantanagroup.link;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class FhirBundler {
  protected static final Logger logger = LoggerFactory.getLogger(FhirBundler.class);

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
    List<String> patientDataReferences = patientMeasureReport.getEvaluatedResource().stream()
            .filter(er -> {
              if (er.getReference() == null || er.getReference().isEmpty()) {
                return false;
              }

              // Evaluated resources that don't have the populationReference extension did fully satisfy
              // the criteria of at least one CQL definition
              if (er.getExtensionsByUrl(Constants.ExtensionPopulationReference).isEmpty()) {
                return false;
              }

              // Ignore contained references
              if (er.getReference() != null && er.getReference().startsWith("#")) {
                return false;
              }

              return true;
            })
            .map(er -> {
              return er.getReference();
            }).collect(Collectors.toList());

    // Return a list of all the resources identified by evaluated resources
    List<DomainResource> patientResources = new ArrayList<>();

    for (String patientDataReference : patientDataReferences) {
      Optional<Bundle.BundleEntryComponent> found = patientDataBundle.getEntry().stream().filter(e -> {
        String thisReference = e.getResource().getResourceType().toString() + "/" + e.getResource().getIdElement().getIdPart();
        return thisReference.equals(patientDataReference);
      }).findFirst();

      if (found.isPresent()) {
        patientResources.add(FhirHelper.cleanResource((DomainResource) found.get().getResource(), this.fhirDataProvider.ctx));
      } else {
        logger.error(String.format("Could not find resource %s for in bundle %s", patientDataReference, patientDataBundle.getId()));
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

    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.COLLECTION);
    bundle.setTimestamp(new Date());
    bundle.setMeta(meta);

    bundle.getIdentifier()
            .setSystem(Constants.IdentifierSystem)
            .setValue(UUID.randomUUID().toString());

    // Add the master measure report to the bundle
    bundle.addEntry().setResource(FhirHelper.cleanResource(masterMeasureReport, this.fhirDataProvider.ctx));

    // Add census list(s) to the report bundle
    List<ListResource> censusLists = FhirHelper.getCensusLists(documentReference, this.fhirDataProvider);
    bundle.getEntry().addAll(censusLists.stream().map(censusList -> {
      Bundle.BundleEntryComponent newCensusListEntry = new Bundle.BundleEntryComponent();
      newCensusListEntry.setResource(censusList);
      return newCensusListEntry;
    }).collect(Collectors.toList()));

    // If configured to include all resources...
    if (includePopulationSubjectResults) {
      // Get the references to the individual patient measure reports from the master
      List<String> patientMeasureReportReferences = FhirHelper.getPatientMeasureReportReferences(masterMeasureReport);

      // Retrieve the individual patient measure reports from the server
      List<MeasureReport> patientReports = FhirHelper.getPatientReports(patientMeasureReportReferences, fhirDataProvider);

      for (MeasureReport patientMeasureReport : patientReports) {
        MeasureReport clonedPatientMeasureReport = (MeasureReport) FhirHelper.cleanResource(patientMeasureReport, this.fhirDataProvider.ctx);

        if (removeContainedEvaluatedResources) {
          this.removeContainedEvaluatedResource(clonedPatientMeasureReport);
        }

        // Add the individual patient measure report to the bundle
        bundle.addEntry().setResource(clonedPatientMeasureReport);

        // Get all the evaluated resources in the individual patient measure reports and add them to the bundle
        List<DomainResource> patientResources = this.getPatientResources(masterMeasureReport.getIdElement().getIdPart(), patientMeasureReport);

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
