package com.lantanagroup.link.config.query;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class USCoreQueryParametersResourceConfig {
  private String resourceType;
  private List<USCoreQueryParametersResourceParameterConfig> parameters;
}

