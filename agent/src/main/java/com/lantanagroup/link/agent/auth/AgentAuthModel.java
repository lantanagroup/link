package com.lantanagroup.link.agent.auth;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AgentAuthModel {
  private String authorization;
  private String remoteAddress;
  private String forwardedRemoteAddress;
}
