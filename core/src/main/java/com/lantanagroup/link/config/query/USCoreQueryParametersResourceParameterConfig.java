package com.lantanagroup.link.config.query;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class USCoreQueryParametersResourceParameterConfig {
  private String name;
  private List<String> values;
}
