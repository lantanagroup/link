package com.lantanagroup.link;

import lombok.Getter;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FhirBundleProcessor {
  private final Bundle bundle;

  @Getter
  private final Bundle.BundleEntryComponent linkOrganization;

  @Getter
  private final Bundle.BundleEntryComponent linkDevice;

  @Getter
  private final Bundle.BundleEntryComponent linkQueryPlanLibrary;

  @Getter
  private final List<Bundle.BundleEntryComponent> linkCensusLists;

  @Getter
  private final HashMap<String, List<Bundle.BundleEntryComponent>> patientResources = new HashMap<>();

  @Getter
  private final List<Bundle.BundleEntryComponent> aggregateMeasureReports;

  public FhirBundleProcessor(Bundle bundle) {
    this.bundle = bundle;

    this.linkOrganization = this.bundle.getEntry().stream()
            .filter(e -> {
              return e.getResource().getResourceType().equals(ResourceType.Organization) &&
                      e.getResource().getMeta().hasProfile(Constants.SubmittingOrganizationProfile);
            })
            .findFirst()
            .orElse(null);

    this.linkDevice = this.bundle.getEntry().stream()
            .filter(e -> {
              return e.getResource().getResourceType().equals(ResourceType.Device) &&
                      e.getResource().getMeta().hasProfile(Constants.SubmittingDeviceProfile);
            })
            .findFirst()
            .orElse(null);

    this.linkQueryPlanLibrary = this.bundle.getEntry().stream()
            .filter(e -> e.getResource().getResourceType().equals(ResourceType.Library))
            .filter(e -> {
              Library library = (Library) e.getResource();
              return library.getType().getCoding().stream()
                      .anyMatch(c -> c.getCode().equals(Constants.LibraryTypeModelDefinitionCode));
            })
            .findFirst()
            .orElse(null);

    this.linkCensusLists = this.bundle.getEntry().stream()
            .filter(e ->
                    e.getResource().getResourceType().equals(ResourceType.List) &&
                            e.getResource().getMeta().getProfile().stream()
                                    .anyMatch(p -> p.getValue().equals(Constants.CensusProfileUrl)))
            .sorted(new FhirBundlerEntrySorter.ResourceComparator())
            .collect(Collectors.toList());

    for (Bundle.BundleEntryComponent e : bundle.getEntry()) {
      String patientReference = FhirHelper.getPatientReference(e.getResource());
      if (patientReference != null) {
        String patientId = patientReference.replace("Patient/", "");
        if (!this.patientResources.containsKey(patientId)) {
          this.patientResources.put(patientId, new ArrayList<>());
        }
        this.patientResources.get(patientId).add(e);
      }
    }

    this.aggregateMeasureReports = this.bundle.getEntry().stream()
            .filter(e -> e.getResource().getResourceType().equals(ResourceType.MeasureReport) && ((MeasureReport) e.getResource()).getType().equals(MeasureReport.MeasureReportType.SUBJECTLIST))
            .sorted(new FhirBundlerEntrySorter.ResourceComparator())
            .collect(Collectors.toList());
  }

  private static boolean isSameResource(Resource r1, Resource r2) {
    if (r1 != null && r2 != null) {
      return r1.getResourceType().equals(r2.getResourceType()) && r1.getIdElement().getIdPart().equals(r2.getIdElement().getIdPart());
    } else return r1 == null && r2 == null;
  }

  public List<String> getPatientIds() {
    return this.getPatientResources()
            .keySet().stream()
            .sorted().collect(Collectors.toList());
  }

  public Stream<Bundle.BundleEntryComponent> getOtherResources() {
    return bundle.getEntry().stream()
            .filter(r -> {
              if (isSameResource(r.getResource(), this.linkOrganization.getResource())) {
                return false;
              } else if (isSameResource(r.getResource(), this.linkDevice.getResource())) {
                return false;
              } else if (this.linkQueryPlanLibrary == null || isSameResource(r.getResource(), this.linkQueryPlanLibrary.getResource())) {
                return false;
              } else if (this.linkCensusLists.stream().anyMatch(l -> isSameResource(r.getResource(), l.getResource()))) {
                return false;
              } else if (this.patientResources.values().stream().anyMatch(l -> l.stream().anyMatch(p -> isSameResource(r.getResource(), p.getResource())))) {
                return false;
              } else return this.aggregateMeasureReports.stream().noneMatch(l -> isSameResource(r.getResource(), l.getResource()));
            })
            .sorted(new FhirBundlerEntrySorter.ResourceComparator());
  }
}
