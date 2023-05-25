package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BulkResponse {
  private String transactionTime;
  private String request;
  private boolean requireAccessToken;
  private List<BulkOutput> output;
}
