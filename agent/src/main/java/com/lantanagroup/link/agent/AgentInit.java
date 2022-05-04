package com.lantanagroup.link.agent;

import com.lantanagroup.link.agent.controller.AgentController;
import com.lantanagroup.link.config.agent.AgentConfig;
import com.lantanagroup.link.config.query.QueryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class AgentInit {

  @Autowired
  private AgentConfig config;

  @Autowired
  private QueryConfig queryConfig;

  private static final Logger logger = LoggerFactory.getLogger(AgentController.class);

  private void init() {
    if (this.queryConfig.isRequireHttps() && !this.queryConfig.getFhirServerBase().toLowerCase().startsWith("https://")) {
      logger.error("Error, Query URL requires https");
      return;
    }  
  }
}
