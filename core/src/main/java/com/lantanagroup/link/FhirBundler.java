package com.lantanagroup.link;

import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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
            .map(e -> (DomainResource) e.getResource())
            .collect(Collectors.toList());
  }

  public Bundle generateBundle(boolean allResources, MeasureReport masterMeasureReport) {
    Meta meta = new Meta();
    Coding tag = meta.addTag();
    tag.setCode(Constants.REPORT_BUNDLE_TAG);
    tag.setSystem(Constants.MainSystem);

    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.COLLECTION);
    bundle.setMeta(meta);

    // Add the master measure report to the bundle
    bundle.addEntry().setResource(masterMeasureReport);

    if (allResources) {
      // Get the references to the individual patient measure reports from the master
      List<String> patientMeasureReportReferences = FhirHelper.getPatientMeasureReportReferences(masterMeasureReport);

      // Retrieve the individual patient measure reports from the server
      List<MeasureReport> patientReports = FhirHelper.getPatientReports(patientMeasureReportReferences, fhirDataProvider);

      for (MeasureReport patientMeasureReport : patientReports) {
        // Add the individual patient measure report to the bundle
        bundle.addEntry().setResource(patientMeasureReport);

        // Get all the evaluated resources in the individual patient measure reports and add them to the bundle
        List<DomainResource> patientResources = this.getPatientResources(patientMeasureReport);

        for (DomainResource patientResource : patientResources) {
          bundle.addEntry().setResource(patientResource);
        }
      }
    }

    // Bundle is complete
    return bundle;
  }
}
