package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ValidationCategory {
  private String id;
  private String title;
  private ValidationCategorySeverities severity;
  private Boolean acceptable;
  private String guidance;

  public ValidationCategory(String title, ValidationCategorySeverities severity, Boolean acceptable, String guidance) {
    this.title = title;
    this.severity = severity;
    this.acceptable = acceptable;
    this.guidance = guidance;
  }
}
