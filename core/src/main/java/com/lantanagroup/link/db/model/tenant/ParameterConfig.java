package com.lantanagroup.link.db.model.tenant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParameterConfig {
  private String name;
  private String literal;
  private String variable;
  private String format;
  private String ids;
  private int paged;
}
