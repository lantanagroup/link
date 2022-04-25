package com.lantanagroup.link.config.api;

import com.lantanagroup.link.config.auth.LinkOAuthConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class ApiReportDefsConfig {
  /**
   * <strong>api.report-defs.max-retry</strong><br>The number of times the API should try to retrieve and store the report definitions.
   */
  public Integer maxRetry;

  /**
   * <strong>api.report-defs.retry-wait</strong><br>The number of milliseconds the API should wait in between retries.
   */
  public Integer retryWait;

  /**
   * <strong>api.report-defs.urls</strong><br>A list of the URLs for each report definition that should be supported by the system.
   */
  public List<String> urls;

  /**
   * <strong>api.report-defs.auth</strong><br>A list of authentication properties.
   */
  public LinkOAuthConfig auth;
}
