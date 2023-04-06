package com.lantanagroup.link.db.model;

import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

import java.util.Date;

@Getter
@Setter
public class Audit {
  private String id = (new ObjectId()).toString();
  private Date timestamp = new Date();
  private String tenantId;
  private String userId;
  private String name;
  private String network;
  private AuditTypes type;
  private String notes;
}
