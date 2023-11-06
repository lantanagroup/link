package com.lantanagroup.link.db.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class SubmissionStatus {
  private UUID id = UUID.randomUUID();
  private String tenantId;
  private String reportId;
  private SubmissionStatusTypes status;
  private Date startDate;
  private Date endDate;
  private List<String> measureIds;
}
