package com.lantanagroup.link.db.model.tenant.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CernerAuth {
  private String tokenUrl;
  private String clientId;
  private String secret;
  private String scopes;
}
