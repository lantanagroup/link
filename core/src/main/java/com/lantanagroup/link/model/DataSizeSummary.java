package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Getter
@Setter
public class DataSizeSummary {
  private List<Object> data;
  private double totalSize;
  private int count;
  private double averageSize;
  private double minSize;
  private double maxSize;
  private LocalDateTime startDate;
  private LocalDateTime endDate;

  private HashMap<String, HashMap<String,Integer>> countDataType = new HashMap<>();
  private HashMap<String, HashMap<String, Double>> averageDataSize = new HashMap<>();
}
