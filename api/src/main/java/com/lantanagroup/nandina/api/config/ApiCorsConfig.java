package com.lantanagroup.nandina.api.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiCorsConfig {
  private String allowedOrigins;
  private String allowedHeaders;
  private Boolean allowedCredentials;
  private String[] allowedMethods;
}
