package com.lantanagroup.link.db.model.tenant;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
public class QueryList {
  private String fhirServerBase;

  @Size(min = 1)
  private List<EhrPatientList> lists;
}
