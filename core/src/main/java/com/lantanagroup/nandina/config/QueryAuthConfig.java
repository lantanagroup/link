package com.lantanagroup.nandina.config;

import com.lantanagroup.nandina.config.AuthConfigModes;
import com.lantanagroup.nandina.config.IAuthConfig;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Getter @Setter
@Validated
public class QueryAuthConfig implements IAuthConfig {
  @NotNull
  private AuthConfigModes authMode;

  private String username;
  private String password;
  private String scopes;
  private String token;

  @URL
  private String tokenUrl;
}
