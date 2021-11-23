package com.lantanagroup.link.config.consumer;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DataSourceConfig {
  private String url;
  private String username;
  private String password;
  private String driverClassName;
  private String hibernateDialect;
}
