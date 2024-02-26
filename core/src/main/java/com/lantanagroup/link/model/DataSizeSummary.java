package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Getter
@Setter
public class DataSizeSummary {
  private List<Object> Data;
  private double TotalSize;
  private int Count;
  private double AverageSize;
  private double MinSize;
  private double MaxSize;
  private LocalDateTime startDate;
  private LocalDateTime endDate;

  private HashMap<String, HashMap<String,Integer>> CountDataType = new HashMap<>();
  private HashMap<String, HashMap<String, Double>> AverageDataSize = new HashMap<>();
}
