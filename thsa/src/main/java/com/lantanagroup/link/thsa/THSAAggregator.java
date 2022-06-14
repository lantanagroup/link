package com.lantanagroup.link.thsa;

import com.lantanagroup.link.GenericAggregator;
import com.lantanagroup.link.IReportAggregator;
import org.hl7.fhir.r4.model.MeasureReport;

import java.util.List;

public class THSAAggregator extends GenericAggregator implements IReportAggregator {

  @Override
  protected void aggregatePatientReports(MeasureReport masterMeasureReport, List<MeasureReport> patientMeasureReports) {
    for (MeasureReport patientMeasureReport : patientMeasureReports) {
      for (MeasureReport.MeasureReportGroupComponent group : patientMeasureReport.getGroup()) {
        for (MeasureReport.MeasureReportGroupPopulationComponent population : group.getPopulation()) {
          // Check if group and population code exist in master, if not create
          MeasureReport.MeasureReportGroupPopulationComponent measureGroupPopulation = getOrCreateGroupAndPopulation(masterMeasureReport, population, group);
          // Add population.count to the master group/population count
          measureGroupPopulation.setCount(measureGroupPopulation.getCount() + population.getCount());
        }
      }
    }
  }
}
