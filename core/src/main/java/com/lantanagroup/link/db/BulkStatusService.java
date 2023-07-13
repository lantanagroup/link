package com.lantanagroup.link.db;

import com.lantanagroup.link.db.model.BulkStatus;
import com.lantanagroup.link.db.model.BulkStatusResult;
import com.lantanagroup.link.db.model.BulkStatuses;
import com.lantanagroup.link.db.model.tenant.Tenant;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class BulkStatusService {
  private static final Logger logger = LoggerFactory.getLogger(BulkStatusService.class);

  @Getter
  private Tenant config;
//  private MongoDatabase database;

  public static final String BULK_DATA_COLLECTION = "bulkDataStatus";
  public static final String BULK_DATA_STATUS_RESPONSE_COLLECTION = "bulkDataStatusResponse";

  protected BulkStatusService(Tenant config) {
    this.config = config;
  }

  public static BulkStatusService create(SharedService sharedService, String tenantId) {
    throw new UnsupportedOperationException("Service does not support SQL Server");

//    if(StringUtils.isEmpty(tenantId)) {
//      return null;
//    }
//
//    Tenant tenant = sharedService.getTenantConfig(tenantId);
//
//    if (tenant == null) {
//      logger.error("Tenant {} not found", tenantId);
//      return null;
//    }
//
//    return new BulkStatusService(sharedService.getClient().getDatabase(tenant.getDatabase()), tenant);
  }

//  public MongoCollection<BulkStatus> getBulkStatusCollection() {
//    var collection =  this.database.getCollection(BULK_DATA_COLLECTION, BulkStatus.class);
//    return collection;
//  }

//  public MongoCollection<BulkStatusResult> getBulkStatusResultCollection() {
//    return this.database.getCollection(BULK_DATA_STATUS_RESPONSE_COLLECTION, BulkStatusResult.class);
//  }

  public BulkStatus saveBulkStatus(BulkStatus bulkStatus) {
    throw new UnsupportedOperationException();
//    Bson criteria = eq("_id", bulkStatus.getId());
//    var result = this.getBulkStatusCollection().replaceOne(criteria, bulkStatus, new ReplaceOptions().upsert(true));
////    if(result.wasAcknowledged()) {
////      bulkStatus.setId(result.getUpsertedId().toString());
////    }
//    return bulkStatus;
  }

  public List<BulkStatus> getBulkStatusesByTenantId(String tenantId) {
    throw new UnsupportedOperationException();
//    List<BulkStatus> bulkStatuses = new ArrayList<>();
//    this.getBulkStatusCollection()
//            .find(eq("tenantId", tenantId))
//            .into(bulkStatuses);
//    return bulkStatuses;
  }

  public BulkStatus getBulkStatusById(String id) {
    throw new UnsupportedOperationException();
//    return this.getBulkStatusCollection()
//            .find(eq("id", id))
//            .first();
  }

  public List<BulkStatus> getBulkStatusByStatus(String status) {
    throw new UnsupportedOperationException();
//    List<BulkStatus> bulkStatuses = new ArrayList<>();
//    Bson criteria = eq("status", status);
//    this.getBulkStatusCollection()
//            .find(criteria)
//            .into(bulkStatuses);
//    return bulkStatuses;
  }

  public List<BulkStatus> getBulkPendingStatusesWithPopulatedUrl() {
    throw new UnsupportedOperationException();
//    List<BulkStatus> bulkStatuses = new ArrayList<>();
//    Bson criteria = and(
//            eq("status", BulkStatuses.pending),
//            ne("statusUrl", null)
//    );
//    this.getBulkStatusCollection()
//            .find(criteria)
//            .into(bulkStatuses);
//    return bulkStatuses;
  }

  public BulkStatusResult saveResult(BulkStatusResult result) {
    throw new UnsupportedOperationException();
//    Bson criteria = or(eq("id", result.getId()));
//    var saveResult = this.getBulkStatusResultCollection().replaceOne(criteria, result, new ReplaceOptions().upsert(true));
//    if(saveResult.wasAcknowledged()) {
//      result.setId(saveResult.getUpsertedId().toString());
//    }
//    return result;
  }

  public BulkStatusResult getResultById(String id) {
    throw new UnsupportedOperationException();
//    return this.getBulkStatusResultCollection()
//            .find(eq("id", id))
//            .first();
  }

  public BulkStatusResult getResultByStatusId(String statusId) {
    throw new UnsupportedOperationException();
//    return this.getBulkStatusResultCollection()
//            .find(eq("statusId", statusId))
//            .first();
  }
}
