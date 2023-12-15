package com.lantanagroup.link.db.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class Query {
  private UUID id;
  private String reportId;
  private String queryType;
  private String url;
  private String body;
  private Date retrieved;
}
