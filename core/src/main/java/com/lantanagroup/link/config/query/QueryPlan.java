package com.lantanagroup.link.config.query;

import lombok.Getter;
import lombok.Setter;

import java.time.Period;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class QueryPlan {
  private Period lookback = Period.ZERO;
  private List<TypedQueryPlan> initial = Collections.emptyList();
  private List<TypedQueryPlan> supplemental = Collections.emptyList();
}
