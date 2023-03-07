package com.lantanagroup.link.config.nhsn;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "nhsn.reporting-plan")
public class ReportingPlanConfig {
  private boolean enabled;
  private String url;
  private String nhsnOrgId;
  private Map<String, String> planNames = Map.of();
}
