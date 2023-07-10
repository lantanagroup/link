package com.lantanagroup.link.db.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class Audit {
  private UUID id = UUID.randomUUID();
  private Date timestamp = new Date();
  private String tenantId;
  private String userId;
  private String name;
  private String network;
  private AuditTypes type;
  private String notes;
}
