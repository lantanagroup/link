package com.lantanagroup.link.nhsn;

import com.lantanagroup.link.GenericAggregator;
import com.lantanagroup.link.IReportAggregator;
import com.lantanagroup.link.model.ReportContext;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Primary
public class ReportAggregator extends GenericAggregator implements IReportAggregator {
  private static final Logger logger = LoggerFactory.getLogger(GenericAggregator.class);

  protected void addSubjectResult(
          MeasureReport individualMeasureReport,
          MeasureReport.MeasureReportGroupPopulationComponent aggregatePopulation) {
    logger.debug(
            "Adding subject result to {}: {}",
            aggregatePopulation.getCode().getCodingFirstRep().getCode(),
            individualMeasureReport.getId());
    ListResource subjectResults;
    if (aggregatePopulation.hasSubjectResults()) {
      subjectResults = aggregatePopulation.getSubjectResultsTarget();
    } else {
      subjectResults = new ListResource()
              .setStatus(ListResource.ListStatus.CURRENT)
              .setMode(ListResource.ListMode.SNAPSHOT);
      aggregatePopulation.getSubjectResults().setResource(subjectResults);
      aggregatePopulation.setSubjectResultsTarget(subjectResults);
    }
    subjectResults.addEntry().getItem().setReference(
            "MeasureReport/" + individualMeasureReport.getIdElement().getIdPart());
  }

  public void aggregatePatientReport(MeasureReport masterMeasureReport, MeasureReport measureReport) {
    for (MeasureReport.MeasureReportGroupComponent group : measureReport.getGroup()) {
      for (MeasureReport.MeasureReportGroupPopulationComponent population : group.getPopulation()) {
        // Check if group and population code exist in master, if not create
        MeasureReport.MeasureReportGroupPopulationComponent measureGroupPopulation = getOrCreateGroupAndPopulation(masterMeasureReport, population, group);
        // Add population.count to the master group/population count
        measureGroupPopulation.setCount(measureGroupPopulation.getCount() + population.getCount());
        // If this population incremented the master
        if (population.getCount() > 0) {
          // add subject results
          addSubjectResult(measureReport, measureGroupPopulation);
        }
      }
    }
  }

  public void finishAggregation(MeasureReport masterMeasureReport) {
    for (MeasureReport.MeasureReportGroupComponent group : masterMeasureReport.getGroup()) {
      for (MeasureReport.MeasureReportGroupPopulationComponent population : group.getPopulation()) {
        logger.debug(
                "Subject result count for {}: {}",
                population.getCode().getCodingFirstRep().getCode(),
                population.hasSubjectResults() ? population.getSubjectResultsTarget().getEntry().size() : 0);
        logger.debug(
                "Population count for {}: {}",
                population.getCode().getCodingFirstRep().getCode(),
                population.getCount());
      }
    }
  }

  protected void createGroupsFromMeasure(MeasureReport masterMeasureReport, ReportContext.MeasureContext measureContext) {
    // if there are no groups generated then gets them from the measure
    if (masterMeasureReport.getGroup().size() == 0) {
      Bundle bundle = measureContext.getReportDefBundle();
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

