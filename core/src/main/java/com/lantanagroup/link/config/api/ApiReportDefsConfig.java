package com.lantanagroup.link.config.api;

import com.lantanagroup.link.config.auth.LinkOAuthConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class ApiReportDefsConfig {
  /**
   * <strong>api.report-defs.max-retry</strong><br>The number of times the API should try to retrieve and store the measures.
   */
  public Integer maxRetry = 5;

  /**
   * <strong>api.report-defs.retry-wait</strong><br>The number of milliseconds the API should wait in between retries.
   */
  public Integer retryWait = 5000;

  /**
   * <strong>api.report-defs.urls</strong><br>A list of report definitions for each measure that should be supported by the system.
   */
  public List<ApiReportDefsUrlsConfig> urls;

  /**
   * <strong>api.report-defs.auth</strong><br>A list of authentication properties.
   */
  public LinkOAuthConfig auth;
}
