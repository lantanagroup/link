package com.lantanagroup.link.csv;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CSVEntry {
  private String group;
  private String population;
  private Integer populationCount;
  private String stratifier;
  private String stratum;
  private String stratumPopulation;
  private Integer stratumPopulationCount;
}
