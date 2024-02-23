package com.lantanagroup.link.model;
import com.lantanagroup.link.db.model.PatientMeasureReportSize;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;

@Getter
@Setter
public class PatientMeasureReportSizeSummary{
  private List<PatientMeasureReportSize> Reports;
  private Double TotalSize;
  private int ReportCount;
  private Double AverageReportSize;

  private HashMap<String, Integer> CountReportSizeByMeasureId= new HashMap<>();
  private HashMap<String, Double> AverageReportSizeByMeasureId = new HashMap<>();

  private HashMap<String, Integer> CountReportSizeByPatientId= new HashMap<>();
  private HashMap<String, Double> AverageReportSizeByPatientId= new HashMap<>();

  private HashMap<String, Integer> CountReportSizeByReportId= new HashMap<>();
  private HashMap<String, Double> AverageReportSizeByReportId= new HashMap<>();
}
