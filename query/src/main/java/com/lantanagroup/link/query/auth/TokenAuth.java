package com.lantanagroup.link.query.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@SuppressWarnings("unused")
@Component
public class TokenAuth implements ICustomAuth {
  @Autowired
  private TokenAuthConfig config;

  @Override
  public String getAuthHeader() {
    return this.config.getToken();
  }

  @Override
  public String getApiKeyHeader() {
    return null;
  }
}
