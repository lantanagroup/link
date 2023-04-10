package com.lantanagroup.link.query.auth;

import com.lantanagroup.link.db.TenantService;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@SuppressWarnings("unused")
@Component
public class TokenAuth implements ICustomAuth {
  private static final Logger logger = LoggerFactory.getLogger(TokenAuth.class);

  @Setter
  private TenantService tenantService;

  @Override
  public String getAuthHeader() {
    if (this.tenantService.getConfig().getFhirQuery() == null) {
      logger.error("Tenant {} not configured to query FHIR", this.tenantService.getConfig().getId());
      return null;
    }

    if (this.tenantService.getConfig().getFhirQuery().getTokenAuth() == null) {
      logger.error("Tenant {} not configured for Epic authentication", this.tenantService.getConfig().getId());
      return null;
    }

    return this.tenantService.getConfig().getFhirQuery().getTokenAuth().getToken();
  }

  @Override
  public String getApiKeyHeader() {
    return null;
  }
}
