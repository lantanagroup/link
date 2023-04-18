package com.lantanagroup.link.db;

import com.lantanagroup.link.db.model.BulkStatus;
import com.lantanagroup.link.db.model.BulkStatuses;
import com.lantanagroup.link.db.model.tenant.Tenant;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BulkDataService {
  private static final Logger logger = LoggerFactory.getLogger(BulkDataService.class);

  @Getter
  private Tenant config;
  private MongoDatabase database;

  public static final String BULK_DATA_COLLECTION = "bulkDataStatus";

  protected BulkDataService(MongoDatabase database, Tenant config) {
    this.database = database;
    this.config = config;
  }

  public static BulkDataService create(SharedService sharedService, String tenantId) {
    if(StringUtils.isEmpty(tenantId)) {
      return null;
    }

    Tenant tenant = sharedService.getTenantConfig(tenantId);

    if (tenant == null) {
      logger.error("Tenant {} not found", tenantId);
      return null;
    }

    return new BulkDataService(sharedService.getClient().getDatabase(tenant.getDatabase()), tenant);
  }

  public MongoCollection<BulkStatus> getBulkStatusCollection() {
    return this.database.getCollection(BULK_DATA_COLLECTION, BulkStatus.class);
  }


}
