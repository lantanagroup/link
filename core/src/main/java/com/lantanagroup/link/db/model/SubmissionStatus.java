package com.lantanagroup.link.db.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class SubmissionStatus {
  private UUID id = UUID.randomUUID();
  private String tenantId;
  private String bundleId;
  private SubmissionStatusTypes status;
  private Date startDate;
  private Date endDate;
}
