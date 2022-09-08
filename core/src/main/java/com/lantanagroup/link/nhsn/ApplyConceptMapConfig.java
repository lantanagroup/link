package com.lantanagroup.link.nhsn;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ApplyConceptMapConfig {
  private String conceptMapId;
  private List<String> fhirPathContexts;
}
