package com.lantanagroup.link.db;

import com.lantanagroup.link.db.model.BulkStatus;
import com.lantanagroup.link.db.model.BulkStatusResult;
import com.lantanagroup.link.db.model.BulkStatuses;
import com.lantanagroup.link.db.model.tenant.Tenant;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.include;

public class BulkStatusService {
  private static final Logger logger = LoggerFactory.getLogger(BulkStatusService.class);

  @Getter
  private Tenant config;
  private MongoDatabase database;

  public static final String BULK_DATA_COLLECTION = "bulkDataStatus";
  public static final String BULK_DATA_STATUS_RESPONSE_COLLECTION = "bulkDataStatusResponse";

  protected BulkStatusService(MongoDatabase database, Tenant config) {
    this.database = database;
    this.config = config;
  }

  public static BulkStatusService create(SharedService sharedService, String tenantId) {
    if(StringUtils.isEmpty(tenantId)) {
      return null;
    }

    Tenant tenant = sharedService.getTenantConfig(tenantId);

    if (tenant == null) {
      logger.error("Tenant {} not found", tenantId);
      return null;
    }

    return new BulkStatusService(sharedService.getClient().getDatabase(tenant.getDatabase()), tenant);
  }

  public MongoCollection<BulkStatus> getBulkStatusCollection() {
    var collection =  this.database.getCollection(BULK_DATA_COLLECTION, BulkStatus.class);
    return collection;
  }

  public MongoCollection<BulkStatusResult> getBulkStatusResultCollection() {
    return this.database.getCollection(BULK_DATA_STATUS_RESPONSE_COLLECTION, BulkStatusResult.class);
  }

  public BulkStatus saveBulkStatus(BulkStatus bulkStatus) {
    Bson criteria = eq("_id", bulkStatus.getId());
    var result = this.getBulkStatusCollection().replaceOne(criteria, bulkStatus, new ReplaceOptions().upsert(true));
//    if(result.wasAcknowledged()) {
//      bulkStatus.setId(result.getUpsertedId().toString());
//    }
    return bulkStatus;
  }

  public List<BulkStatus> getBulkStatusesByTenantId(String tenantId) {
    List<BulkStatus> bulkStatuses = new ArrayList<>();
    this.getBulkStatusCollection()
            .find(eq("tenantId", tenantId))
            .into(bulkStatuses);
    return bulkStatuses;
  }

  public BulkStatus getBulkStatusById(String id) {
    return this.getBulkStatusCollection()
            .find(eq("id", id))
            .first();
  }

  public List<BulkStatus> getBulkStatusByStatus(String status) {
    List<BulkStatus> bulkStatuses = new ArrayList<>();
    Bson criteria = eq("status", status);
    this.getBulkStatusCollection()
            .find(criteria)
            .into(bulkStatuses);
    return bulkStatuses;
  }

  public List<BulkStatus> getBulkPendingStatusesWithPopulatedUrl() {
    List<BulkStatus> bulkStatuses = new ArrayList<>();
    Bson criteria = and(
            eq("status", BulkStatuses.pending),
            ne("statusUrl", null)
    );
    this.getBulkStatusCollection()
            .find(criteria)
            .into(bulkStatuses);
    return bulkStatuses;
  }

  public BulkStatusResult saveResult(BulkStatusResult result) {
    Bson criteria = or(eq("id", result.getId()));
    var saveResult = this.getBulkStatusResultCollection().replaceOne(criteria, result, new ReplaceOptions().upsert(true));
    if(saveResult.wasAcknowledged()) {
      result.setId(saveResult.getUpsertedId().toString());
    }
    return result;
  }

  public BulkStatusResult getResultById(String id) {
    return this.getBulkStatusResultCollection()
            .find(eq("id", id))
            .first();
  }

  public BulkStatusResult getResultByStatusId(String statusId) {
    return this.getBulkStatusResultCollection()
            .find(eq("statusId", statusId))
            .first();
  }
}
