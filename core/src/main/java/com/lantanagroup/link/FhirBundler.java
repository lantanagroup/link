package com.lantanagroup.link;

import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FhirBundler {
  protected static final Logger logger = LoggerFactory.getLogger(FhirBundler.class);

  private FhirDataProvider fhirDataProvider;

  public FhirBundler(FhirDataProvider fhirDataProvider) {
    this.fhirDataProvider = fhirDataProvider;
  }

  /**
   * Traverse each population in the measure report and find the subject results of the population,
   * which is a reference to a contained List resource. The contained List resource contains each of the
   * individual patient measure reports that is used to calculate the aggregate value of the population.
   *
   * @param masterMeasureReport The master measure report to search for lists of individual reports
   * @return The list of unique references to individual patient MeasureReport resources that comprise the master
   */
  public List<String> getPatientMeasureReportReferences(MeasureReport masterMeasureReport) {
    List<String> references = new ArrayList<>();

    // Loop through the groups and populations within each group
    // Look for a reference to a contained List resource representing the measure reports
    // that comprise the group/population's aggregate
    for (MeasureReport.MeasureReportGroupComponent group : masterMeasureReport.getGroup()) {
      for (MeasureReport.MeasureReportGroupPopulationComponent population : group.getPopulation()) {
        String populateListRef = population.getSubjectResults().getReference();
        Optional<ListResource> populationList = masterMeasureReport
                .getContained().stream()
                .filter(c -> c.getIdElement().getIdPart().equals(populateListRef))
                .map(c -> (ListResource) c)
                .findFirst();

        // If a contained List resource was found, extract each MeasureReport reference from the list
        if (populationList.isPresent()) {
          for (ListResource.ListEntryComponent listEntry : populationList.get().getEntry()) {
            String individualReportRef = listEntry.getItem().getReference();

            // Should only be references to MeasureReport. Skip if not.
            if (!individualReportRef.startsWith("MeasureReport/")) {
              continue;
            }

            // Only add the references to the list of it is not already in the list (create a unique list of MR references)
            if (!references.contains(listEntry.getItem().getReference())) {
              references.add(listEntry.getItem().getReference());
            }
          }
        }
      }
    }

    return references;
  }

  private List<MeasureReport> getPatientReports(List<String> patientMeasureReportReferences) {
    Bundle patientReportsReqBundle = new Bundle();
    patientReportsReqBundle.setType(Bundle.BundleType.TRANSACTION);

    for (String patientMeasureReportReference : patientMeasureReportReferences) {
      patientReportsReqBundle.addEntry().getRequest()
              .setMethod(Bundle.HTTPVerb.GET)
              .setUrl(patientMeasureReportReference);
    }

    // Get each of the individual patient measure reports
    return this.fhirDataProvider.transaction(patientReportsReqBundle)
            .getEntry().stream()
            .map(e -> (MeasureReport) e.getResource())
            .collect(Collectors.toList());
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
      List<String> patientMeasureReportReferences = this.getPatientMeasureReportReferences(masterMeasureReport);

      // Retrieve the individual patient measure reports from the server
      List<MeasureReport> patientReports = this.getPatientReports(patientMeasureReportReferences);

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
