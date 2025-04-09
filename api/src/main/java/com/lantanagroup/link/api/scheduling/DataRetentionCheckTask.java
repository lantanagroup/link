package com.lantanagroup.link.api.scheduling;

import com.lantanagroup.link.api.controller.DataController;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class DataRetentionCheckTask implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(DataRetentionCheckTask.class);

  @Autowired
  private DataController dataController;

  @Setter
  private String tenantId;

  @Override
  public void run() {
    try {
      this.dataController.expungeData(this.tenantId, null, null);
      logger.info("Done executing data retention check scheduled task");
    } catch (Exception ex) {
      logger.error("Error running data retention check scheduled task", ex);
    }
  }
}
