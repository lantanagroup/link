package com.lantanagroup.link.db.model.tenant.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureAuth {
  private String tokenUrl;
  private String clientId;
  private String secret;
  private String resource;
}
