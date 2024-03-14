package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TenantSummary {
  private String id;
  private String name;
  private String nhsnOrgId;
  private String lastSubmissionId;
  private String lastSubmissionDate;
  private List<TenantSummaryMeasure> measures = new ArrayList<>();
}
