package com.lantanagroup.link.db.model.tenant;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ReportingPlan {
  private boolean enabled;
  private String url;
  private String nhsnOrgId;
  private Map<String, String> planNames = Map.of();
}
