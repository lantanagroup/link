package com.lantanagroup.link.db.model.tenant;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class TypedQueryPlan {
  private String resourceType;
  private List<ParameterConfig> parameters = Collections.emptyList();
  private ReferencesConfig references;
  private boolean earlyExit;
}
