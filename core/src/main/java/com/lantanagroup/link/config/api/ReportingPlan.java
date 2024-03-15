package com.lantanagroup.link.config.api;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ReportingPlan {
  private boolean enabled;
  private SamsAuth samsAuth = new SamsAuth();
  private String url;
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
