package com.lantanagroup.link.agent;
import com.lantanagroup.link.config.agent.AgentConfig;
import com.lantanagroup.link.config.query.QueryConfig;
import org.springframework.beans.factory.annotation.Autowired;

public class AgentInit {

  @Autowired
  private AgentConfig config;

  @Autowired
  private QueryConfig queryConfig;

  private void init() {
    if (this.queryConfig.isRequireHttps()) {
      // TODO: Validate HTTPS for URLs in queryConfig
    }
  }
}
