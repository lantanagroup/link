package com.lantanagroup.link;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthConfig {

  private String tokenUrl;
  private String user;
  private String pass;
  private String scope;
}

