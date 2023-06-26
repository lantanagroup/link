package com.lantanagroup.link.db.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

import java.util.Date;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkStatus {
  private String id = (new ObjectId()).toString();
  private String tenantId;
  private String statusUrl;
  private String status;
  private String errorMessage;
  private Date lastChecked;
}

