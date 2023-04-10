package com.lantanagroup.link.db.model.tenant.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EpicAuth {
  private String key;
  private String tokenUrl;
  private String clientId;
  private String audience;
}
