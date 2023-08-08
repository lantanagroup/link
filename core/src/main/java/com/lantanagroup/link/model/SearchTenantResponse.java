package com.lantanagroup.link.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SearchTenantResponse {
  private String id;
  private String name;
  private String retentionPeriod;
  private String cdcOrgId;
}
