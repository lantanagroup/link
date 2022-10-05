package com.lantanagroup.link.query.auth;

public interface ICustomAuth {
  String getAuthHeader() throws Exception;
  String getApiKeyHeader() throws Exception;
}
