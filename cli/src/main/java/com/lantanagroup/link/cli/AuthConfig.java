package com.lantanagroup.link.cli;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthConfig {

  private String tokenUrl;
  private String user;
  private String pass;
  private String scope;
  private String clientId;
  private String clientSecret;
  private String credentialMode;
}

