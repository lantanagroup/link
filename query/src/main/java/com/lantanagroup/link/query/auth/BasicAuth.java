package com.lantanagroup.link.query.auth;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BasicAuth implements ICustomAuth {
  private static final Logger logger = LoggerFactory.getLogger(BasicAuth.class);

  @Autowired
  private BasicAuthConfig config;

  @Override
  public String getAuthHeader() {
    String username = this.config.getUsername();
    String password = this.config.getPassword();
    logger.debug("Using basic credentials for FHIR authentication with username " + username);
    String credentials = username + ":" + password;
    String encoded = Base64.encodeBase64String(credentials.getBytes());
    return "Basic " + encoded;
  }

  @Override
  public String getApiKeyHeader() throws Exception {
    return null;
  }

}
