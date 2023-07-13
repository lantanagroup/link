package com.lantanagroup.link.config.api;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@Configuration
@Validated
public class ApiDataStoreConfig {
  @NotBlank(message = "Base URL for Data Store is required.")
  private String baseUrl;

  @NotBlank(message = "Data Store Username is required.")
  private String username;

  @NotBlank(message = "Data Store Password is required.")
  private String password;

  private String socketTimeout;
}
