package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReportBase {
  private String id;
  private List<String> measureIds;
  private String periodStart;
  private String periodEnd;
}
