package com.lantanagroup.link.config.api;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class ApiReportDefsConfig {
  public Integer maxRetry;
  public Integer retryWait;
  public List<String> urls;
}
