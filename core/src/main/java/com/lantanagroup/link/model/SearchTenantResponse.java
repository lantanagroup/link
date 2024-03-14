package com.lantanagroup.link.model;

import com.lantanagroup.link.db.model.tenant.TenantVendors;
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
  private TenantVendors vendor;
  private String otherVendor;
}
