package com.lantanagroup.link.csv;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.MeasureReport;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MeasureReportToCSV {
  private String getValue(CodeableConcept code) {
    if (code == null) return null;

    Coding coding = code.getCodingFirstRep();
    if (coding != null && StringUtils.isNotEmpty(coding.getCode())) {
      return String.format("%s|%s", coding.getSystem() != null ? coding.getSystem() : "", coding.getCode());
    }

    return code.getText();
  }

  private String getCellValue(String value) {
    if (value != null) {
      String rep = value.replace("\"", "\\\"");

      if (value.indexOf(" ") >= 0) {
        return "\"" + rep + "\"";
      } else {
        return rep;
      }
    }

    return "";
  }

  public String convert(MeasureReport measureReport) {
    List<CSVEntry> csvEntries = new ArrayList<>();

    measureReport.getGroup().forEach(group -> {
      String groupCode = this.getValue(group.getCode());

      if (StringUtils.isEmpty(groupCode) && StringUtils.isNotEmpty(group.getId())) {
        groupCode = "id:" + group.getId();
      }

      for (MeasureReport.MeasureReportGroupPopulationComponent population : group.getPopulation()) {
        CSVEntry popEntry = new CSVEntry();
        popEntry.setGroup(groupCode);
        popEntry.setPopulation(this.getValue(population.getCode()));
        popEntry.setPopulationCount(population.getCount());
        csvEntries.add(popEntry);
      }

      for (MeasureReport.MeasureReportGroupStratifierComponent stratifier : group.getStratifier()) {
        for (MeasureReport.StratifierGroupComponent stratum : stratifier.getStratum()) {
          for (MeasureReport.StratifierGroupPopulationComponent stratumPopulation : stratum.getPopulation()) {
            CSVEntry stratifierEntry = new CSVEntry();
            stratifierEntry.setGroup(groupCode);
            stratifierEntry.setStratifier(this.getValue(stratifier.getCodeFirstRep()));
            stratifierEntry.setStratum(this.getValue(stratum.getValue()));
            stratifierEntry.setStratumPopulation(this.getValue(stratumPopulation.getCode()));
            stratifierEntry.setStratumPopulationCount(stratumPopulation.getCount());
            csvEntries.add(stratifierEntry);
          }
        }
      }
    });

    List<String> lines = csvEntries.stream().map(cl -> {
      List<String> cells = new ArrayList<>();
      cells.add(this.getCellValue(cl.getGroup()));
      cells.add(this.getCellValue(cl.getPopulation()));
      cells.add(cl.getPopulationCount() != null ? this.getCellValue(String.valueOf(cl.getPopulationCount())) : "");
      cells.add(this.getCellValue(cl.getStratifier()));
      cells.add(this.getCellValue(cl.getStratum()));
      cells.add(this.getCellValue(cl.getStratumPopulation()));
      cells.add(cl.getStratumPopulationCount() != null ? this.getCellValue(String.valueOf(cl.getStratumPopulationCount())) : "");
      return String.join(",", cells);
    }).collect(Collectors.toList());

    return "group,population,populationCount,stratifier,stratum,stratumPopulation,stratumPopulationCount\n" + String.join("\n", lines);
  }
}
