package com.lantanagroup.link.db.model.tenant.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BasicAuthAndApiKey {
  private String username;
  private String password;
  private String apikey;
}
