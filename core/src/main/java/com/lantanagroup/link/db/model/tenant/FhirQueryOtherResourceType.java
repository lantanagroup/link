package com.lantanagroup.link.db.model.tenant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FhirQueryOtherResourceType {
  private String resourceType;
  private Boolean supportsSearch = false;
  private Integer countPerSearch = 100;
}
