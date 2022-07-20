package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.GenericAggregator;
import com.lantanagroup.link.IReportAggregator;
import com.lantanagroup.link.model.ReportContext;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ReportAggregator extends GenericAggregator implements IReportAggregator {

  private Resource getOrCreateContainedList(MeasureReport master, String code) {
    // find the list by code
    Optional<Resource> resource = master.getContained().stream().filter(resourceList -> resourceList.getId().contains(code)).findFirst();
    // create the list if not found
    if (resource.isEmpty()) {
      ListResource listResource = new ListResource();
      listResource.setId(code + "-subject-list");
      listResource.setStatus(ListResource.ListStatus.CURRENT);
      listResource.setMode(ListResource.ListMode.SNAPSHOT);
      master.getContained().add(listResource);
      return listResource;
    }
    return resource.get();
  }

  private void addSubjectResults(MeasureReport.MeasureReportGroupPopulationComponent population, MeasureReport.MeasureReportGroupPopulationComponent measureGroupPopulation) {
    measureGroupPopulation.setSubjectResults(new Reference());
    String populationCode = population.getCode().getCoding().size() > 0 ? population.getCode().getCoding().get(0).getCode() : "";
    measureGroupPopulation.getSubjectResults().setReference("#" + populationCode + "-subject-list");
  }

  private void addMeasureReportReferences(MeasureReport patientMeasureReport, ListResource listResource) {
    ListResource.ListEntryComponent listEntry = new ListResource.ListEntryComponent();
    listEntry.setItem(new Reference());
    listEntry.getItem().setReference("MeasureReport/" + patientMeasureReport.getId());
    listResource.addEntry(listEntry);
  }


  public void aggregatePatientReports(MeasureReport masterMeasureReport, List<MeasureReport> patientMeasureReports) {
    // agregate all individual reports in one
    for (MeasureReport patientMeasureReport : patientMeasureReports) {
      for (MeasureReport.MeasureReportGroupComponent group : patientMeasureReport.getGroup()) {
        for (MeasureReport.MeasureReportGroupPopulationComponent population : group.getPopulation()) {
          // Check if group and population code exist in master, if not create
          MeasureReport.MeasureReportGroupPopulationComponent measureGroupPopulation = getOrCreateGroupAndPopulation(masterMeasureReport, population, group);
          // Add population.count to the master group/population count
          measureGroupPopulation.setCount(measureGroupPopulation.getCount() + population.getCount());
          // If this population incremented the master
          if (population.getCount() > 0) {
            // add subject results
            addSubjectResults(population, measureGroupPopulation);
            // Identify or create the List for this master group/population
            ListResource listResource = (ListResource) getOrCreateContainedList(masterMeasureReport, population.getCode().getCoding().get(0).getCode());
            // add this patient measure report to the contained List
            addMeasureReportReferences(patientMeasureReport, listResource);
          }
        }
      }
    }
  }

  protected void createGroupsFromMeasure(MeasureReport masterMeasureReport, ReportContext context) {
    // if there are no groups generated then gets them from the measure
    if (masterMeasureReport.getGroup().size() == 0) {
      Bundle bundle = context.getReportDefBundle();
      Optional<Bundle.BundleEntryComponent> measureEntry = bundle.getEntry().stream()
              .filter(e -> e.getResource().getResourceType() == ResourceType.Measure)
              .findFirst();

      if (measureEntry.isPresent()) {
        Measure measure = (Measure) measureEntry.get().getResource();
        measure.getGroup().forEach(group -> {
          MeasureReport.MeasureReportGroupComponent groupComponent = new MeasureReport.MeasureReportGroupComponent();
          groupComponent.setCode(group.getCode());
          group.getPopulation().forEach(population -> {
            MeasureReport.MeasureReportGroupPopulationComponent populationComponent = new MeasureReport.MeasureReportGroupPopulationComponent();
            populationComponent.setCode(population.getCode());
            populationComponent.setCount(0);
            groupComponent.addPopulation(populationComponent);

          });
          masterMeasureReport.addGroup(groupComponent);
        });
      }
    }
  }
}
