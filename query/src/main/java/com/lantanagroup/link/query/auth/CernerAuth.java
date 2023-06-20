package com.lantanagroup.link.query.auth;

import com.lantanagroup.link.auth.OAuth2Helper;
import com.lantanagroup.link.db.TenantService;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CernerAuth implements ICustomAuth {
  private static final Logger logger = LoggerFactory.getLogger(CernerAuth.class);

  @Setter
  private TenantService tenantService;

  @Override
  public String getAuthHeader() {
    if (this.tenantService.getConfig().getFhirQuery() == null) {
      logger.error("Tenant {} not configured to query FHIR", this.tenantService.getConfig().getId());
      return null;
    }

    if (this.tenantService.getConfig().getFhirQuery().getCernerAuth() == null) {
      logger.error("Tenant {} not configured for Cerner authentication", this.tenantService.getConfig().getId());
      return null;
    }

    logger.debug("Using OAuth2 to retrieve a system token for FHIR authentication");
    String token = OAuth2Helper.getClientCredentialsToken(
            this.tenantService.getConfig().getFhirQuery().getCernerAuth().getTokenUrl(),
            this.tenantService.getConfig().getFhirQuery().getCernerAuth().getClientId(),
            this.tenantService.getConfig().getFhirQuery().getCernerAuth().getSecret(),
            this.tenantService.getConfig().getFhirQuery().getCernerAuth().getScopes(),
            true);

    if (!StringUtils.isEmpty(token)) {
      logger.debug("Retrieved system token for FHIR authentication: " + token);
      return "Bearer " + token;
    } else {
      logger.error("No system token to use for FHIR authentication");
    }

    return null;
  }

  @Override
  public String getApiKeyHeader() throws Exception {
    return null;
  }
}
