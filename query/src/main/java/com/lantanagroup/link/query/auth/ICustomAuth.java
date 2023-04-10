package com.lantanagroup.link.query.auth;

import com.lantanagroup.link.db.TenantService;

public interface ICustomAuth {
  String getAuthHeader() throws Exception;

  String getApiKeyHeader() throws Exception;

  void setTenantService(TenantService tenantService);
}
