package com.lantanagroup.nandina.config;

public interface IAuthConfig {
  AuthConfigModes getAuthMode();

  String getUsername();

  String getPassword();

  String getTokenUrl();

  String getScopes();

  String getToken();
}
