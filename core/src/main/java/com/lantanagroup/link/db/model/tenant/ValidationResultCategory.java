package com.lantanagroup.link.db.model.tenant;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ValidationResultCategory {
  private UUID id;
  private UUID validationResultId;
  private String categoryCode;
}
