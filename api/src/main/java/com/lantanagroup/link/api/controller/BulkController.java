package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.api.bulk.BulkManagerService;
import com.lantanagroup.link.db.BulkStatusService;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.model.BulkStatus;
import com.lantanagroup.link.db.model.BulkStatuses;
import com.lantanagroup.link.db.model.tenant.Tenant;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/{tenantId}/bulk")
public class BulkController  extends BaseController {

  private static final Logger logger = LoggerFactory.getLogger(BulkController.class);

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private SharedService sharedService;

  @Setter
  private ExecutorService executorService;


  public BulkController() {
    this.executorService = Executors.newFixedThreadPool(10);
  }

  // Disallow binding of sensitive attributes
  // Ex: DISALLOWED_FIELDS = new String[]{"details.role", "details.age", "is_admin"};
  final String[] DISALLOWED_FIELDS = new String[]{};

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.setDisallowedFields(DISALLOWED_FIELDS);
  }

  @PostMapping("/$initiate-bulk-data")
  public void initiateBulkDataRequest(@PathVariable String tenantId) throws Exception {
    //initiate a bulk request and save status url to persistence

    BulkStatusService bulkStatusService = BulkStatusService.create(sharedService, tenantId);
    Tenant tenantConfig = sharedService.getTenantConfig(tenantId);

    //log request in datastore
    var bulkStatus = new BulkStatus();
    bulkStatus.setStatus(BulkStatuses.Pending);
    bulkStatus.setLastChecked(new Date());
    bulkStatus.setTenantId(tenantId);

    logger.info(String.format("Saving initial bulk status record for tenant %s as PENDING"), tenantId);

    bulkStatus = bulkStatusService.saveBulkStatus(bulkStatus);

    new BulkManagerService(tenantConfig, executorService, sharedService).InitiateBulkDataRequest(bulkStatus);


  }

  @GetMapping("/status/{id}")
  public BulkStatus getBulkStatusRecord(@PathVariable String id, @PathVariable String tenantId) {
    //retrieve bulk status record for an id
    Tenant tenantConfig = sharedService.getTenantConfig(tenantId);
    BulkStatus status = new BulkManagerService(tenantConfig, executorService, sharedService).getBulkStatusById(id);
    return status;
  }

  @GetMapping("/status")
  public List<BulkStatus> getAllBulkStatuses(@PathVariable String tenantId) {
    //retrieve all pending bulk status requests for a tenantId
    Tenant tenantConfig = sharedService.getTenantConfig(tenantId);
    List<BulkStatus> statuses = new BulkManagerService(tenantConfig, executorService, sharedService).getBulkStatusByTenantId(tenantId);
    return statuses;
  }

  @PostMapping("$execute-query-and-retrieval")
  public void executeQueryAndRetrieval(@PathVariable String tenantId){
    //for use in ApiInit to initiate all pending bulk status requests, can also be called via API
    BulkStatusService bulkStatusService = BulkStatusService.create(sharedService, tenantId);
    Tenant tenantConfig = sharedService.getTenantConfig(tenantId);

    new BulkManagerService(tenantConfig, executorService, sharedService).getPendingRequestsAndGetStatusResults(tenantId);
  }
}
