package com.lantanagroup.nandina.config;

public interface IQueryConfig {
  String getFhirServerBase();
  String getApiKey();
  QueryAuthConfig getQueryAuth();
}
