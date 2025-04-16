package com.lantanagroup.link.api.bulk;

import com.lantanagroup.link.api.controller.BulkController;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class BulkStatusFetchTask implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(BulkStatusFetchTask.class);

  @Autowired
  private BulkController bulkController;

  @Setter
  private String tenantId;

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
