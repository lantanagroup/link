package com.lantanagroup.nandina.query.pihc.fhir.r4.saner;

import com.lantanagroup.nandina.query.BaseFormQuery;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Resource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FormQuery extends BaseFormQuery {
  private static boolean isPopulationMatch(MeasureReport.MeasureReportGroupPopulationComponent population, String populationCode) {
    List<String> systems = Arrays.asList(Constants.MEASURE_POPULATION_SYSTEMS);
    if (population.getCode() != null && population.getCode().getCoding() != null && population.getCode().getCoding().size() > 0) {
      if (population.getCode().getCoding().get(0).getSystem() != null && population.getCode().getCoding().get(0).getCode() != null) {
        if (!systems.contains(population.getCode().getCoding().get(0).getSystem())) {
          return false;
        }

        return population.getCode().getCoding().get(0).getCode().equalsIgnoreCase(populationCode);
      }
    }

    return false;
  }

  private static boolean isGroupMatch(MeasureReport.MeasureReportGroupComponent group, String groupCode) {
    if (group.getCode() != null && group.getCode().getCoding().size() > 0) {
      if (group.getCode().getCoding().get(0).getSystem() != null && group.getCode().getCoding().get(0).getCode() != null) {
        return group.getCode().getCoding().get(0).getSystem().equals(Constants.MEASURE_GROUP_SYSTEM) &&
                group.getCode().getCoding().get(0).getCode().equals(groupCode);
      }
    }

    return false;
  }

  private Integer countForPopulation(Map<String, Resource> data, String populationCode) {
    Integer total = null;

    if (data != null) {
      for (Resource resource : data.values()) {
        MeasureReport mr = (MeasureReport) resource;

        for (MeasureReport.MeasureReportGroupComponent group : mr.getGroup()) {
          for (MeasureReport.MeasureReportGroupPopulationComponent population : group.getPopulation()) {
            if (!isPopulationMatch(population, populationCode)) continue;

            if (population.getCountElement() != null && population.getCountElement().getValue() != null) {
              total = (total != null ? total : 0) + population.getCount();
            }
          }
        }
      }
    }

    return total;
  }

  @Override
  public void execute() {
    if (!this.getContextData().containsKey("measureReportData")) {
      throw new IllegalArgumentException("measureReportData");
    }

    Map<String, Resource> data = (Map<String, Resource>) this.getContextData("measureReportData");

    this.setAnswer("numC19HospPats", this.countForPopulation(data, "numC19HospPats"));
    this.setAnswer("numC19MechVentPats", this.countForPopulation(data, "numC19MechVentPats"));
    this.setAnswer("numC19HOPats", this.countForPopulation(data, "numC19HOPats"));
    this.setAnswer("numC19OverflowPats", this.countForPopulation(data, "numC19OverflowPats"));
    this.setAnswer("numC19OFMechVentPats", this.countForPopulation(data, "numC19OFMechVentPats"));
    this.setAnswer("numC19Died", this.countForPopulation(data, "numC19Died"));
    this.setAnswer("numTotBeds", this.countForPopulation(data, "numTotBeds"));
    this.setAnswer("numBeds", this.countForPopulation(data, "numbeds"));
    this.setAnswer("numBedsOcc", this.countForPopulation(data, "numBedsOcc"));
    this.setAnswer("numICUBeds", this.countForPopulation(data, "numICUBeds"));
    this.setAnswer("numICUBedsOcc", this.countForPopulation(data, "numICUBedsOcc"));
    this.setAnswer("mechanicalVentilators", this.countForPopulation(data, "numVent"));
    this.setAnswer("mechanicalVentilatorsUsed", this.countForPopulation(data, "numVentUse"));
  }
}
