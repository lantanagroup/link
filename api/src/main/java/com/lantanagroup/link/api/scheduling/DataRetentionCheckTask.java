package com.lantanagroup.link.api.scheduling;

import com.lantanagroup.link.api.controller.ReportDataController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DataRetentionCheckTask implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(DataRetentionCheckTask.class);

  @Autowired
  private ReportDataController reportDataController;

  @Override
  public void run() {
    try {
      this.reportDataController.expungeData();
      logger.info("Done executing data retention check scheduled task");
    } catch (Exception ex) {
      logger.error("Error running data retention check scheduled task", ex);
    }
  }
}
