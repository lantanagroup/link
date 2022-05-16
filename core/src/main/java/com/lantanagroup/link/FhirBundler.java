package com.lantanagroup.link;

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

  private List<DomainResource> getPatientResources(MeasureReport patientMeasureReport) {
    Bundle retrievePatientData = new Bundle();
    retrievePatientData.setType(Bundle.BundleType.BATCH);

    // Add all references to evaluated resources to the bundle as entries with a request method & url
    retrievePatientData.getEntry().addAll(patientMeasureReport.getEvaluatedResource().stream().map(er -> {
      Bundle.BundleEntryComponent newEntry = new Bundle.BundleEntryComponent();
      newEntry.getRequest()
              .setMethod(Bundle.HTTPVerb.GET)
              .setUrl(er.getReference());
      return newEntry;
    }).collect(Collectors.toList()));

    // Execute the patient data transaction bundle to retrieve all the patient data
    Bundle patientDataBundle = this.fhirDataProvider.transaction(retrievePatientData);

    // Look for and log issues with retrieving patient data
    List<String> issues = patientDataBundle.getEntry().stream()
            .filter(e -> e.getResource() != null && e.getResource().getResourceType() == ResourceType.OperationOutcome)
            .map(e -> {
              OperationOutcome oo = (OperationOutcome) e.getResource();
              return String.join("\n", oo.getIssue().stream().map(i -> i.getDiagnostics()).collect(Collectors.toList()));
            }).collect(Collectors.toList());

    if (issues.size() > 0) {
      logger.error(String.format("Error retrieving patient data due to:\n%s", String.join("\n", issues)));
    }

    // Return a list of all the resources that are not OperationOutcomes (issues)
    return patientDataBundle.getEntry().stream()
            .filter(e -> e.getResource() != null && e.getResource().getResourceType() != ResourceType.OperationOutcome)
            .map(e -> FhirHelper.cleanResource((DomainResource) e.getResource(), this.fhirDataProvider.ctx))
            .collect(Collectors.toList());
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
   * @param allResources
   * @param masterMeasureReport
   * @return
   */
  public Bundle generateBundle(boolean allResources, MeasureReport masterMeasureReport, DocumentReference documentReference) {
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
    if (allResources) {
      // Get the references to the individual patient measure reports from the master
      List<String> patientMeasureReportReferences = FhirHelper.getPatientMeasureReportReferences(masterMeasureReport);

      // Retrieve the individual patient measure reports from the server
      List<MeasureReport> patientReports = FhirHelper.getPatientReports(patientMeasureReportReferences, fhirDataProvider);

      for (MeasureReport patientMeasureReport : patientReports) {
        MeasureReport clonedPatientMeasureReport = (MeasureReport) FhirHelper.cleanResource(patientMeasureReport, this.fhirDataProvider.ctx);

        this.removeContainedEvaluatedResource(clonedPatientMeasureReport);

        // Add the individual patient measure report to the bundle
        bundle.addEntry().setResource(clonedPatientMeasureReport);

        // Get all the evaluated resources in the individual patient measure reports and add them to the bundle
        List<DomainResource> patientResources = this.getPatientResources(patientMeasureReport);

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
