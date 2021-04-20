package com.lantanagroup.nandina.query.api.auth;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class QueryApiAuthModel {
  private String authorization;
  private String remoteAddress;
}
