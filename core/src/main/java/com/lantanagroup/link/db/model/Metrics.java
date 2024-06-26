package com.lantanagroup.link.db.model;


import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class Metrics {
  private UUID id = UUID.randomUUID();
  private String tenantId;
  private String reportId;
  private String version;
  private String category;
  private String taskName;
  private Date timestamp;
  private MetricData data;
}

