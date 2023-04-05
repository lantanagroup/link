package com.lantanagroup.link;

import com.lantanagroup.link.config.TenantConfig;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TenantService {
  private static final Logger logger = LoggerFactory.getLogger(TenantService.class);

  @Getter
  private TenantConfig config;

  private MongoDatabase database;

  public TenantService(MongoDatabase database, TenantConfig config) {
    this.database = database;
    this.config = config;
  }
}
