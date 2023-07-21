package com.lantanagroup.link.api.bulk;

import com.lantanagroup.link.db.BulkStatusService;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.BulkStatus;
import com.lantanagroup.link.db.model.BulkStatusResult;
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

  public BulkManagerService(Tenant tenantConfig, ExecutorService executorService, SharedService sharedService, ApplicationContext applicationContext) {
    this.tenantConfig = tenantConfig;
    this.executorService = executorService;
    this.sharedService = sharedService;
    this.applicationContext = applicationContext;
  }

  public void InitiateBulkDataRequest(BulkStatus status) throws Exception {

    //status.setStatus(BulkStatuses.pending);
    BulkStatusService bulkStatusService = BulkStatusService.create(sharedService, tenantConfig.getId());
    //bulkStatusService.saveBulkStatus(status);

    TenantService tenantService = TenantService.create(sharedService, tenantConfig.getId());
    BulkQuery bulkQuery = new BulkQuery();
    bulkQuery.executeInitiateRequest(tenantService, bulkStatusService, status, this.applicationContext);
  }

  public void getPendingRequestsAndGetStatusResults(String tenantId) {
    //get pending statuses
    BulkStatusService bulkStatusService = BulkStatusService.create(sharedService, tenantConfig.getId());
    var statuses = bulkStatusService.getBulkPendingStatusesWithPopulatedUrl();

    //loop through statuses and fire off thread to get results
    for(var status : statuses) {
      try{
        executorService.execute(() -> {
          BulkQuery bulkQuery = new BulkQuery();
          status.setStatus(BulkStatuses.inProgress);
          bulkStatusService.saveBulkStatus(status);
          BulkStatusResult statusResult = null;
          try {
            statusResult = bulkQuery.getStatus(status, TenantService.create(sharedService, tenantId), bulkStatusService, this.applicationContext);
          } catch (Exception e) {
            status.setStatus(BulkStatuses.pending);
            bulkStatusService.saveBulkStatus(status);
            throw new RuntimeException(e);
          }

          if(statusResult != null){
            try {
              bulkQuery.getResultSetFromBulkResultAndLoadPatientData(statusResult,TenantService.create(sharedService, tenantId),this.applicationContext);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }

        });
      } catch (Exception e) {
        logger.error(e.getMessage());
        status.setStatus(BulkStatuses.pending);
        bulkStatusService.saveBulkStatus(status);
        throw new RuntimeException(e);
      }
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

  public BulkStatusResult getBulkStatusResultByStatusId(String id){
    BulkStatusService bulkStatusService = BulkStatusService.create(sharedService, tenantConfig.getId());
    BulkStatusResult result = bulkStatusService.getResultByStatusId(id);
    return result;
  }
}
