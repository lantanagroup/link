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
  private TenantService tenantService;
  @Setter
  private BulkStatusService bulkStatusService;
  @Setter
  ApplicationContext applicationContext;

  public BulkManagerService(Tenant tenantConfig, ExecutorService executorService, SharedService sharedService, ApplicationContext applicationContext) {
    this.tenantConfig = tenantConfig;
    this.executorService = executorService;
    this.sharedService = sharedService;
    this.applicationContext = applicationContext;
    this.tenantService = TenantService.create(sharedService, tenantConfig.getId());
    this.bulkStatusService = BulkStatusService.create(tenantConfig);
  }

  public void InitiateBulkDataRequest(BulkStatus status) throws Exception {
    BulkQuery bulkQuery = new BulkQuery();
    bulkQuery.executeInitiateRequest(tenantService, bulkStatusService, status, this.applicationContext);
  }

  public void getPendingRequestsAndGetStatusResults(String tenantId) {
    //get pending statuses
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

  public List<BulkStatus> getBulkStatuses() {
    List<BulkStatus> status = bulkStatusService.getBulkStatuses();
    return status;
  }

  public BulkStatus getBulkStatusById(String id) {
    BulkStatus status = bulkStatusService.getBulkStatusById(id);
    return status;
  }

  public BulkStatusResult getBulkStatusResultByStatusId(String id){
    return bulkStatusService
            .getBulkStatusResults()
            .stream()
            .filter(r -> r.getStatusId()
            .equals(id))
            .findFirst()
            .orElse(null);
  }
}
