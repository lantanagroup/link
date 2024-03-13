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

  @Getter
  private final HashMap<Integer, String> bundleEntryIndexToFileMap = new HashMap<>();

  public FhirBundleProcessor(Bundle bundle) {
    this.bundle = bundle;

    Bundle.BundleEntryComponent linkOrganization = null;
    Bundle.BundleEntryComponent linkDevice = null;
    Bundle.BundleEntryComponent linkQueryPlanLibrary = null;
    List<Bundle.BundleEntryComponent> linkCensusLists = new ArrayList<>();
    List<Bundle.BundleEntryComponent> aggregateMeasureReports = new ArrayList<>();

    for (int index = 0; index < bundle.getEntry().size(); index++)  {
      Bundle.BundleEntryComponent e = bundle.getEntry().get(index);
      Resource resource = e.getResource();
      ResourceType resourceType = resource.getResourceType();
      String resourceId = resource.getIdElement().getIdPart();
      String patientReference = FhirHelper.getPatientReference(resource);
      if(resourceType.equals(ResourceType.Organization) &&
              resource.getMeta().hasProfile(Constants.SubmittingOrganizationProfile)){
        if(linkOrganization == null){
          linkOrganization = e;
          bundleEntryIndexToFileMap.put(index, Constants.ORGANIZATION_FILE_NAME);
        }
      }
      else if(resourceType.equals(ResourceType.Device) &&
              resource.getMeta().hasProfile(Constants.SubmittingDeviceProfile)){
        if(linkDevice == null){
          linkDevice = e;
          bundleEntryIndexToFileMap.put(index, Constants.DEVICE_FILE_NAME);
        }
      }
      else if(resourceType.equals(ResourceType.Library) && ((Library) resource).getType().getCoding().stream()
              .anyMatch(c -> c.getCode().equals(Constants.LibraryTypeModelDefinitionCode))){
        if(linkQueryPlanLibrary == null){
          linkQueryPlanLibrary = e;
          bundleEntryIndexToFileMap.put(index, String.format("census-%s.json", resourceId));
        }
      }
      else if(resourceType.equals(ResourceType.List) &&
              resource.getMeta().getProfile().stream()
                      .anyMatch(p -> p.getValue().equals(Constants.CensusProfileUrl))){
        linkCensusLists.add(e);
        bundleEntryIndexToFileMap.put(index, String.format("census-%s.json",
                resource.getIdElement().getIdPart()));
      } else if(resourceType.equals(ResourceType.MeasureReport)
              && ((MeasureReport) resource).getType().equals(MeasureReport.MeasureReportType.SUBJECTLIST)) {
        aggregateMeasureReports.add(e);
        bundleEntryIndexToFileMap.put(index, String.format("aggregate-%s.json", resourceId));
      } else if (patientReference != null) {
        String patientId = patientReference.replace("Patient/", "");
        if (!this.patientResources.containsKey(patientId)) {
          this.patientResources.put(patientId, new ArrayList<>());
        }
        this.patientResources.get(patientId).add(e);
        bundleEntryIndexToFileMap.put(index, String.format("patient-%s.json", patientId));
      } else {
        this.otherResources.add(e);
        bundleEntryIndexToFileMap.put(index, "other-resources.json");
      }
    }

    //Cleanup
    this.linkOrganization = linkOrganization;
    this.linkDevice = linkDevice;
    this.linkQueryPlanLibrary = linkQueryPlanLibrary;
    this.linkCensusLists = linkCensusLists.stream()
            .sorted(new FhirBundlerEntrySorter.ResourceComparator()).collect(Collectors.toList());
    this.aggregateMeasureReports = aggregateMeasureReports.stream()
            .sorted(new FhirBundlerEntrySorter.ResourceComparator()).collect(Collectors.toList());

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
