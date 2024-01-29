package com.lantanagroup.link;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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
  private final List<Bundle.BundleEntryComponent> aggregateMeasureReports;

  @Getter
  private final HashMap<String, List<Bundle.BundleEntryComponent>> patientResources = new HashMap<>();

  @Getter
  private final List<Bundle.BundleEntryComponent> otherResources = new ArrayList<>();

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

    this.aggregateMeasureReports = this.bundle.getEntry().stream()
            .filter(e -> e.getResource().getResourceType().equals(ResourceType.MeasureReport) && ((MeasureReport) e.getResource()).getType().equals(MeasureReport.MeasureReportType.SUBJECTLIST))
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
      } else if (isOtherResource(e)) {
        this.otherResources.add(e);
      }
    }
  }

  private static boolean isSameResource(Bundle.BundleEntryComponent e1, Bundle.BundleEntryComponent e2) {
    if (e1 == null || e2 == null) {
      return e1 == null && e2 == null;
    }
    Resource r1 = e1.getResource();
    Resource r2 = e2.getResource();
    if (r1 == null || r2 == null) {
      return r1 == null && r2 == null;
    }
    return r1.getResourceType() == r2.getResourceType() && StringUtils.equals(r1.getIdPart(), r2.getIdPart());
  }

  public List<String> getPatientIds() {
    return this.getPatientResources()
            .keySet().stream()
            .sorted().collect(Collectors.toList());
  }

  private boolean isOtherResource(Bundle.BundleEntryComponent r) {
    if (isSameResource(r, this.linkOrganization)) {
      return false;
    } else if (isSameResource(r, this.linkDevice)) {
      return false;
    } else if (this.linkQueryPlanLibrary == null || isSameResource(r, this.linkQueryPlanLibrary)) {
      return false;
    } else if (this.linkCensusLists.stream().anyMatch(l -> isSameResource(r, l))) {
      return false;
    } else return this.aggregateMeasureReports.stream().noneMatch(l -> isSameResource(r, l));
  }
}
