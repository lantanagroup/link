package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TenantSummaryResponse {
  private int total = 0;
  private List<TenantSummary> tenants = new ArrayList<>();
}
