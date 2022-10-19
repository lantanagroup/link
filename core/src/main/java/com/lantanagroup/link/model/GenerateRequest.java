package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenerateRequest {
  boolean regenerate;
  private String[] bundleIds;
  private String periodStart;
  private String periodEnd;
}
