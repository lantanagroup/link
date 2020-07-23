package com.lantanagroup.nandina.query.fhir.r4.saner;

import com.lantanagroup.nandina.PIHCConstants;
import com.lantanagroup.nandina.query.BaseFormQuery;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Resource;

import java.util.Map;

public class FormQuery extends BaseFormQuery {
  private static boolean isPopulationMatch(MeasureReport.MeasureReportGroupPopulationComponent population, String populationCode) {
    if (population.getCode() != null && population.getCode().getCoding() != null && population.getCode().getCoding().size() > 0) {
      if (population.getCode().getCoding().get(0).getSystem() != null && population.getCode().getCoding().get(0).getCode() != null) {
        return population.getCode().getCoding().get(0).getSystem().equals(Constants.MEASURE_POPULATION_SYSTEM) &&
                population.getCode().getCoding().get(0).getCode().equals(populationCode);
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

  private Integer countForPopulation(Map<String, Resource> data, String groupCode, String populationCode) {
    Integer total = null;

    if (data != null) {
      for (Resource resource : data.values()) {
        MeasureReport mr = (MeasureReport) resource;

        for (MeasureReport.MeasureReportGroupComponent group : mr.getGroup()) {
          if (!isGroupMatch(group, groupCode)) continue;

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

    this.setAnswer(PIHCConstants.HOSPITALIZED, this.countForPopulation(data, "Encounters", "numC19HospPats"));
    this.setAnswer(PIHCConstants.HOSPITALIZED_AND_VENTILATED, this.countForPopulation(data, "Encounters", "numC19MechVentPats"));
    this.setAnswer(PIHCConstants.HOSPITAL_INPATIENT_BEDS, this.countForPopulation(data, "Beds", "numbeds"));
    this.setAnswer(PIHCConstants.HOSPITAL_INPATIENT_BED_OCC, this.countForPopulation(data, "Beds", "numBedsOcc"));
    this.setAnswer(PIHCConstants.HOSPITAL_ICU_BEDS, this.countForPopulation(data, "Beds", "numICUBeds"));
    this.setAnswer(PIHCConstants.HOSPITAL_ICU_BED_OCC, this.countForPopulation(data, "Beds", "numICUBedsOcc"));
    this.setAnswer(PIHCConstants.ED_OVERFLOW, this.countForPopulation(data, "Encounters", "numC19OverflowPats"));
    this.setAnswer(PIHCConstants.ED_OVERFLOW_AND_VENTILATED, this.countForPopulation(data, "Encounters", "numC19OFMechVentPats"));
    this.setAnswer(PIHCConstants.DEATHS, this.countForPopulation(data, "Encounters", "numC19Died"));
    this.setAnswer(PIHCConstants.MECHANICAL_VENTILATORS, this.countForPopulation(data, "Ventilators", "numVent"));
    this.setAnswer(PIHCConstants.MECHANICAL_VENTILATORS_USED, this.countForPopulation(data, "Ventilators", "numVentUse"));
    this.setAnswer(PIHCConstants.HOSPITAL_ONSET, this.countForPopulation(data, "Encounters", "numC19HOPats"));
    this.setAnswer(PIHCConstants.HOSPITAL_BEDS, this.countForPopulation(data, "Beds", "numTotBeds"));
  }
}
