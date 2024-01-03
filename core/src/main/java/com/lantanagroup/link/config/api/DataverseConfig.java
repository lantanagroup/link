package com.lantanagroup.link.config.api;

import lombok.Getter;
import lombok.Setter;

import java.util.Hashtable;

@Getter @Setter
public class DataverseConfig {
  /**
   * The client ID used to authenticate with Dataverse.
   */
  private String clientId;

  /**
   * The secret used to authenticate with Dataverse.
   */
  private String clientSecret;

  /**
   * The org ID within Dataverse that contains the tenant management app.
   */
  private String orgId;

  /**
   * The endpoint to use to get a token from Dataverse.
   */
  private String tokenEndpoint;

  private Hashtable<Integer, String> dqms = new Hashtable<>();
}
