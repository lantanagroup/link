package com.lantanagroup.link;

import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FhirBundler {
  private FhirDataProvider fhirDataProvider;

  public FhirBundler(FhirDataProvider fhirDataProvider) {
    this.fhirDataProvider = fhirDataProvider;
  }

  /**
   * Traverse each population in the measure report and find the subject results of the population,
   * which is a reference to a contained List resource. The contained List resource contains each of the
   * individual patient measure reports that is used to calculate the aggregate value of the population.
   * @param masterMeasureReport
   * @return The list of unique references to individual patient MeasureReport resources that comprise the master
   */
  public List<String> getPatientMeasureReportReferences(MeasureReport masterMeasureReport) {
    List<String> references = new ArrayList<>();

    for (MeasureReport.MeasureReportGroupComponent group : masterMeasureReport.getGroup()) {
      for (MeasureReport.MeasureReportGroupPopulationComponent population : group.getPopulation()) {
        String populateListRef = population.getSubjectResults().getReference();
        ListResource populationList = masterMeasureReport
                .getContained().stream()
                .filter(c -> c.getIdElement().getIdPart().equals(populateListRef))
                .map(c -> (ListResource) c)
                .findFirst().get();

        for (ListResource.ListEntryComponent listEntry : populationList.getEntry()) {
          if (!references.contains(listEntry.getItem().getReference())) {
            references.add(listEntry.getItem().getReference());
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
    // TODO: traverse each evaluatedResource and get a reference

    // TODO: bundle bundle to retrieve each of those referenced resources

    // TODO: execute the transaction bundle against fhir provider

    // TODO: return a list of entry resources
    return null;
  }

  public Bundle generateBundle(boolean allResources, MeasureReport masterMeasureReport) {
    Meta meta = new Meta();
    Coding tag = meta.addTag();
    tag.setCode(Constants.REPORT_BUNDLE_TAG);
    tag.setSystem(Constants.MainSystem);

    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.COLLECTION);
    bundle.setMeta(meta);
    bundle.addEntry().setResource(masterMeasureReport);

    if (allResources) {
      List<String> patientMeasureReportReferences = this.getPatientMeasureReportReferences(masterMeasureReport);
      List<MeasureReport> patientReports = this.getPatientReports(patientMeasureReportReferences);

      for (MeasureReport patientMeasureReport : patientReports) {
        List<DomainResource> patientResources = this.getPatientResources(patientMeasureReport);
        for (DomainResource patientResource : patientResources) {
          bundle.addEntry().setRequest(patientResource);
        }
      }
    }

    return bundle;
  }
}
