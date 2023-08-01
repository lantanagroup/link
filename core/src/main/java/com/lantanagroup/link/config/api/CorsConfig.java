package com.lantanagroup.link.config.api;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CorsConfig {
  /**
   * <strong>api.cors.allowed-origins</strong><br>Space-separated list of allowed origins. Asterisk is allowed.
   */
  private String[] allowedOrigins;

  /**
   * <strong>api.cors.allowed-headers</strong><br>Space-separated list of allowed headers. Asterisk is allowed.
   */
  private String[] allowedHeaders;

  /**
   * <strong>api.cors.allowed-credentials</strong><br>True or false to allow credentials in CORS.
   */
  private Boolean allowedCredentials;

  /**
   * <strong>api.cors.allowed-methods</strong><br>An array of strings representing which methods (ex: GET, PUT, POST) are allowed in CORS requests
   */
  private String[] allowedMethods;
}
