package com.lantanagroup.link.db.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantConfig {
  private String id;

  private String name;

  private String database;

  private TenantBundlingConfig bundling;

  private TenantScheduleConfig scheduling;
}
