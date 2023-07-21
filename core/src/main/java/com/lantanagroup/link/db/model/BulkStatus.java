package com.lantanagroup.link.db.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkStatus {
  private String id = UUID.randomUUID().toString();
  private String tenantId;
  private String statusUrl;
  private String status;
  private String errorMessage;
  private Date lastChecked;
}

