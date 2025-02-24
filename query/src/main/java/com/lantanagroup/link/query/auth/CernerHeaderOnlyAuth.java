package com.lantanagroup.link.query.auth;

import com.lantanagroup.link.auth.OAuth2Helper;
import com.lantanagroup.link.db.TenantService;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CernerHeaderOnlyAuth implements ICustomAuth {
  private static final Logger logger = LoggerFactory.getLogger(CernerHeaderOnlyAuth.class);

  @Setter
  private TenantService tenantService;

  @Override
  public String getAuthHeader() {
    return null;
  }

  @Override
  public String getApiKeyHeader() throws Exception {
    return null;
  }

  @Override
  public String getClientSecretHeader() throws Exception {
    if (this.tenantService.getConfig().getFhirQuery() == null) {
      logger.error("Tenant {} not configured to query FHIR", this.tenantService.getConfig().getId());
      return null;
    }

    if (this.tenantService.getConfig().getFhirQuery().getCernerHeaderOnlyAuth() == null) {
      logger.error("Tenant {} not configured for cerner header only authentication", this.tenantService.getConfig().getId());
      return null;
    }

    return this.tenantService.getConfig().getFhirQuery().getCernerHeaderOnlyAuth().getSecret();
  }

  @Override
  public String getClientIdHeader() throws Exception{
    if (this.tenantService.getConfig().getFhirQuery() == null) {
      logger.error("Tenant {} not configured to query FHIR", this.tenantService.getConfig().getId());
      return null;
    }

    if (this.tenantService.getConfig().getFhirQuery().getCernerHeaderOnlyAuth() == null) {
      logger.error("Tenant {} not configured for cerner header only authentication", this.tenantService.getConfig().getId());
      return null;
    }
    return this.tenantService.getConfig().getFhirQuery().getCernerHeaderOnlyAuth().getClientId();
  }
}
