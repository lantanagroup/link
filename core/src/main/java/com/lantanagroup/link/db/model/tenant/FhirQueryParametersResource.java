package com.lantanagroup.link.db.model.tenant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FhirQueryParametersResource {
  private String resourceType;
  private List<FhirQueryParametersResourceParameter> parameters;
}

