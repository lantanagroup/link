package com.lantanagroup.link.config.parkland;

import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;

import java.util.List;

public class ParklandConfig {

  @Setter
  @Getter
  private List<MeasureReport.MeasureReportGroupComponent> group;

  @Setter
  @Getter
  private List<Measure.MeasureGroupPopulationComponent> population;
}
