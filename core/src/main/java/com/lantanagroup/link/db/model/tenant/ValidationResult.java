package com.lantanagroup.link.db.model.tenant;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ValidationResult {
  private UUID id;
  private String reportId;
  private String code;
  private String details;
  private String severity;
  private String expression;
  private String position;
}
