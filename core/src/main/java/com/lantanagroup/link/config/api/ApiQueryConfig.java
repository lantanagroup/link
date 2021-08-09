package com.lantanagroup.link.config.api;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter @Setter
public class ApiQueryConfig {
  /**
   * <strong>api.query.mode</strong><br> How queries will be run. Locally within the API or remotely on a query agent. Possible values are "Remote" and "Local"
   */
  @NotNull
  private ApiQueryConfigModes mode;

  /**
   * <strong>api.query.url</strong><br>When mode is "Remote", the URL that the API should communicate with to initiate a query
   */
  private String url;

  /**
   * <strong>api.query.api-key</strong><br>When mode is "Remote", an API Key that is required by the QueryAPI to allow requests. This value must match <strong>query.api-key</strong> on the QueryAPI deployment's configuration.
   */
  private String apiKey;
}
