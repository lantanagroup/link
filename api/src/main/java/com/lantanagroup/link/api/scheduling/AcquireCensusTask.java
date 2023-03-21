package com.lantanagroup.link.api.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AcquireCensusTask implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(AcquireCensusTask.class);

  @Override
  public void run() {
    logger.info("Starting task to acquire census");
  }
}
