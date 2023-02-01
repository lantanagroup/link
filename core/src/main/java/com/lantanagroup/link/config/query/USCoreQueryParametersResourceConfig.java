package com.lantanagroup.link.config.query;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class USCoreQueryParametersResourceConfig {
  private String resourceType;
  private List<USCoreQueryParametersResourceParameterConfig> parameters;
}

