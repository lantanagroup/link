package com.lantanagroup.link.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DataverseScheduledReport {
  @JsonProperty("lcg_reportingperiodmethod")
  private Integer reportingPeriodMethod;

  @JsonProperty("lcg_schedule")
  private String schedule;

  @JsonProperty("lcg_regenerate")
  private Boolean regenerate;

  @JsonProperty("lcg_measures")
  private String measures;
}
