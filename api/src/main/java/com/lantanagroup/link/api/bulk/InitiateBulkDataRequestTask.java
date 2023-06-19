package com.lantanagroup.link.api.bulk;

import com.lantanagroup.link.api.controller.BulkController;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InitiateBulkDataRequestTask implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(InitiateBulkDataRequestTask.class);

  @Autowired
  private BulkController bulkController;

  @Setter
  private String tenantId;

  @Override
  public void run() {
    try{
      this.bulkController.initiateBulkDataRequest(this.tenantId);
      logger.info("Done executing bulk data scheduled task");
    } catch(Exception ex){
      logger.error("Error running bulk data scheduled task", ex);
    }
  }
}

