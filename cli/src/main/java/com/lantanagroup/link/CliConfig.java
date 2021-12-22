package com.lantanagroup.link;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "cli")
public class CliConfig {

  private String apiUrl;
  private String periodStart;
  private String periodEnd;
  private AuthConfig auth;
  private String reportTypeId;
}

