package com.lantanagroup.nandina.api.config;

import com.lantanagroup.nandina.config.IQueryConfig;
import com.lantanagroup.nandina.config.QueryAuthConfig;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter @Setter
public class ApiQueryConfig implements IQueryConfig {
  @NotNull
  private ApiQueryConfigModes mode;

  private String url;
  private String apiKey;
  private String fhirServerBase;
  private QueryAuthConfig queryAuth;
}
