package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class GenerateRequest {
  boolean regenerate;
  private String packageId;
  private List<String> bundleIds = new ArrayList<>();
  private String periodStart;
  private String periodEnd;
  boolean validate = true;
}
