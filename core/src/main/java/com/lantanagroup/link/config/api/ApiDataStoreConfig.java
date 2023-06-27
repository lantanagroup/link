package com.lantanagroup.link.config.api;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ApiDataStoreConfig {
  private String baseUrl;
  private String username;
  private String password;
  private String socketTimeout;
}
