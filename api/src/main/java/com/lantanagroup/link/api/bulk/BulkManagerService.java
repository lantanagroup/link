package com.lantanagroup.link.api.bulk;

import com.lantanagroup.link.db.BulkStatusService;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.BulkStatus;
import com.lantanagroup.link.db.model.BulkStatuses;
import com.lantanagroup.link.db.model.tenant.Tenant;
import com.lantanagroup.link.query.uscore.BulkQuery;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class BulkManagerService {
  private static final Logger logger = LoggerFactory.getLogger(InitiateBulkDataRequestTask.class);
  @Setter
  Tenant tenantConfig;
  @Setter
  private ExecutorService executorService;
  @Setter
  private SharedService sharedService;
  @Setter
  ApplicationContext applicationContext;

  public BulkManagerService(Tenant tenantConfig, ExecutorService executorService, SharedService sharedService) {
    this.tenantConfig = tenantConfig;
    this.executorService = executorService;
    this.sharedService = sharedService;
    this.applicationContext = applicationContext;
  }

  public void InitiateBulkDataRequest(BulkStatus status) throws Exception {
    /*
     * 1. update bulk status to in-progress
     * 2. use BulkQuery to initiate request
     * 3. refetch bulkstatus
     * 4. kick off thread CheckBulkDataRequest
     */

    //step 1 - update bulk status to in-progress
    status.setStatus(BulkStatuses.InProgress);
    BulkStatusService bulkStatusService = BulkStatusService.create(sharedService, tenantConfig.getId());
    bulkStatusService.saveBulkStatus(status);

    //step 2 - use BulkQuery to initiate request
    TenantService tenantService = TenantService.create(sharedService, tenantConfig.getId());
    BulkQuery bulkQuery = new BulkQuery();
    bulkQuery.executeInitiateRequest(tenantService, bulkStatusService, status, this.applicationContext);

    //step 3 - refetch bulkstatus
    status = bulkStatusService.getBulkStatusById(status.getId());

    //step 4 - kick off thread CheckBulkDataRequest
    final String tenantId = status.getTenantId();
    executorService.execute(() -> {
      boolean resultsFetched = false;
      while(!resultsFetched){
        BulkStatus bulkStatus = bulkStatusService.getBulkStatusById(tenantId);
        try {
          bulkQuery.getStatus(bulkStatus, tenantService, bulkStatusService, applicationContext);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        bulkStatus = bulkStatusService.getBulkStatusById(tenantId);
        if(bulkStatus.getStatus() == BulkStatuses.Complete)
          resultsFetched = true;
        try {
          Thread.sleep(60000);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  public void getPendingRequestsAndGetStatusResults(String tenantId) {
    //get pending statuses
    BulkStatusService bulkStatusService = BulkStatusService.create(sharedService, tenantConfig.getId());
    var statuses = bulkStatusService.getBulkStatusByStatus(BulkStatuses.Pending);

    //loop through statuses and fire off thread to get results
    for(var status : statuses) {
      executorService.execute(() -> {
        BulkQuery bulkQuery = new BulkQuery();
        try {
          bulkQuery.getStatus(status, TenantService.create(sharedService, tenantId), bulkStatusService, this.applicationContext);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
    }
  }

  public List<BulkStatus> getBulkStatusByTenantId(String tenantId) {
    BulkStatusService bulkStatusService = BulkStatusService.create(sharedService, tenantConfig.getId());
    List<BulkStatus> status = bulkStatusService.getBulkStatusesByTenantId(tenantId);
    return status;
  }

  public BulkStatus getBulkStatusById(String id) {
    BulkStatusService bulkStatusService = BulkStatusService.create(sharedService, tenantConfig.getId());
    BulkStatus status = bulkStatusService.getBulkStatusById(id);
    return status;
  }
}
