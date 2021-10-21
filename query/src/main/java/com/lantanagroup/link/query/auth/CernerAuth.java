package com.lantanagroup.link.query.auth;

import com.lantanagroup.link.auth.OAuth2Helper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CernerAuth implements ICustomAuth {
  private static final Logger logger = LoggerFactory.getLogger(CernerAuth.class);

  @Autowired
  private CernerAuthConfig config;

  @Override
  public String getAuthHeader() {
    logger.debug("Using OAuth2 to retrieve a system token for FHIR authentication");
    String token = OAuth2Helper.getClientCredentialsToken(
            this.config.getTokenUrl(),
            this.config.getClientId(),
            this.config.getSecret(),
            this.config.getScopes());

    if (!StringUtils.isEmpty(token)) {
      logger.debug("Retrieved system token for FHIR authentication: " + token);
      return "Bearer " + token;
    } else {
      logger.error("No system token to use for FHIR authentication");
    }

    return null;
  }
}
