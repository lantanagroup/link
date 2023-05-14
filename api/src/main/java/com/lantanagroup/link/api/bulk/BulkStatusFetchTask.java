package com.lantanagroup.link.api.bulk;

import com.lantanagroup.link.api.controller.BulkController;
import com.lantanagroup.link.api.controller.DataController;
import com.lantanagroup.link.api.scheduling.DataRetentionCheckTask;
import com.lantanagroup.link.db.BulkStatusService;
import com.lantanagroup.link.db.model.tenant.Tenant;
import com.lantanagroup.link.query.uscore.BulkQuery;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BulkStatusFetchTask implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(BulkStatusFetchTask.class);

  @Setter
  private String tenantId;
  @Setter
  private BulkController bulkController;

  @Override
  public void run() {
    try{
      this.bulkController.executeQueryAndRetrieval(this.tenantId);
      logger.info("Done executing bulk data scheduled task");
    } catch(Exception ex){
      logger.error("Error running bulk data scheduled task", ex);
    }
  }
}
