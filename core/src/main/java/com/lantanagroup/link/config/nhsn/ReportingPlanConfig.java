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
  private SamsAuth samsAuth = new SamsAuth();
  private String url;
  private String nhsnOrgId;
  private Map<String, String> planNames = Map.of();

  @Getter
  @Setter
  public static class SamsAuth {
    private String tokenUrl;
    private String username;
    private String password;
    private String emailAddress;
    private String clientId;
    private String clientSecret;
  }
}
