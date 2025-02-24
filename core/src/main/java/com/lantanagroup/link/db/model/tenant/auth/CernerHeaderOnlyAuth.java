package com.lantanagroup.link.db.model.tenant.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CernerHeaderOnlyAuth {
  private String clientId;
  private String secret;
}
