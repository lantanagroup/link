package com.lantanagroup.link.config.api;

import com.lantanagroup.link.config.auth.LinkOAuthConfig;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Min;
import javax.validation.constraints.PositiveOrZero;
import java.util.List;

@Getter @Setter
public class ApiReportDefsConfig {
  /**
   * <strong>api.report-defs.max-retry</strong><br>The number of times the API should try to retrieve and store the measures.
   */
  @Min(1)
  public int maxRetry = 5;

  /**
   * <strong>api.report-defs.retry-wait</strong><br>The number of milliseconds the API should wait in between retries.
   */
  @PositiveOrZero
  public int retryWait = 5000;

  /**
   * <strong>api.report-defs.bundles</strong><br>A list of report definition bundles identifiers and report aggregators which can be used and shoudl be loaded on Evaluation/CQF service.
   */
  public List<ApiReportDefsBundleConfig> bundles;

  /**
   * <strong>api.report-defs.auth</strong><br>A list of authentication properties.
   */
  public LinkOAuthConfig auth;

}
