package com.lantanagroup.link.db.model.tenant;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class QueryPlan {
  private String lookback;
  private List<TypedQueryPlan> initial = Collections.emptyList();
  private List<TypedQueryPlan> supplemental = Collections.emptyList();
}
