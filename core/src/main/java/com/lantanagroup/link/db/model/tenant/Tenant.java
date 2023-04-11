package com.lantanagroup.link.db.model.tenant;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Tenant {
  private String id;

  private String name;

  private String description;

  private String database;

  private Bundling bundling;

  private Schedule scheduling;

  private Events events;

  private QueryList queryList;

  private FhirQuery fhirQuery;

  private ReportingPlan reportingPlan;
}
