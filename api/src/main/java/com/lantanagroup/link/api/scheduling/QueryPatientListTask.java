package com.lantanagroup.link.api.scheduling;

import com.lantanagroup.link.api.controller.PatientIdentifierController;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class QueryPatientListTask implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(QueryPatientListTask.class);

  @Autowired
  private PatientIdentifierController patientIdentifierController;

  @Setter
  private String tenantId;

  @Override
  public void run() {
    logger.info("Starting task to query patient list");

    try {
      this.patientIdentifierController.queryPatientList(tenantId);
      logger.info("Done executing query patient list scheduled task");
    } catch (Exception ex) {
      logger.error("Error querying patient list(s)", ex);
    }
  }
}
