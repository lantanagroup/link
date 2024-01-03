package com.lantanagroup.link.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class DataverseScheduledReports {
  private List<DataverseScheduledReport> value = new ArrayList<>();
}
