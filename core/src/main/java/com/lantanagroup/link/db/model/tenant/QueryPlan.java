package com.lantanagroup.link.db.model.tenant;

import lombok.Getter;
import lombok.Setter;

import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class QueryPlan {
  private String lookback;
  private List<TypedQueryPlan> initial = Collections.emptyList();
  private List<TypedQueryPlan> supplemental = Collections.emptyList();

  public String getLookback() {
    return Objects.requireNonNullElse(lookback, Period.ZERO.toString());
  }
}
