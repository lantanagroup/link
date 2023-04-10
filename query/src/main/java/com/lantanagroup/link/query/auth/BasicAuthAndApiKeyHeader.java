package com.lantanagroup.link.query.auth;

import com.lantanagroup.link.db.TenantService;
import lombok.Setter;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BasicAuthAndApiKeyHeader implements ICustomAuth {
  private static final Logger logger = LoggerFactory.getLogger(BasicAuthAndApiKeyHeader.class);

  @Setter
  private TenantService tenantService;

  @Override
  public String getAuthHeader() {
    if (this.tenantService.getConfig().getFhirQuery() == null) {
      logger.error("Tenant {} not configured to query FHIR", this.tenantService.getConfig().getId());
      return null;
    }

    if (this.tenantService.getConfig().getFhirQuery().getBasicAuthAndApiKey() == null) {
      logger.error("Tenant {} not configured for basic+apikey authentication", this.tenantService.getConfig().getId());
      return null;
    }

    String username = this.tenantService.getConfig().getFhirQuery().getBasicAuthAndApiKey().getUsername();
    String password = this.tenantService.getConfig().getFhirQuery().getBasicAuthAndApiKey().getPassword();
    logger.debug("Using basic credentials for FHIR authentication with username " + username);
    String credentials = username + ":" + password;
    String encoded = Base64.encodeBase64String(credentials.getBytes());
    return "Basic " + encoded;
  }

  @Override
  public String getApiKeyHeader() {
    logger.debug("Adding API key to request");
    return this.tenantService.getConfig().getFhirQuery().getBasicAuthAndApiKey().getApikey();
  }
}
