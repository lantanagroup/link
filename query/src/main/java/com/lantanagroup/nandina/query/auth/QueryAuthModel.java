package com.lantanagroup.nandina.query.auth;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class QueryAuthModel {
  private String authorization;
  private String remoteAddress;
}
