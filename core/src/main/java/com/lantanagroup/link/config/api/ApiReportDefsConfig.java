package com.lantanagroup.link.config.api;

import com.lantanagroup.link.config.auth.LinkOAuthConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class ApiReportDefsConfig {

  /**
   * <strong>api.report-defs.bundles</strong><br>A list of report definition bundles identifiers and report aggregators which can be used and shoudl be loaded on Evaluation/CQF service.
   */
  public List<ApiReportDefsBundleConfig> bundles;

  /**
   * <strong>api.report-defs.auth</strong><br>A list of authentication properties.
   */
  public LinkOAuthConfig auth;

}
