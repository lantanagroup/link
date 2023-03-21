package com.lantanagroup.link.api.scheduling;

import com.lantanagroup.link.api.controller.ReportController;
import com.lantanagroup.link.config.scheduling.ReportingPeriodMethods;
import com.lantanagroup.link.model.GenerateRequest;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GenerateReportTask implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(GenerateReportTask.class);

  @Setter
  private List<String> measureIds;

  @Setter
  private ReportingPeriodMethods reportingPeriodMethod;

  @Setter
  private Boolean regenerateIfExists = false;

  @Autowired
  private ReportController reportController;

  @Override
  public void run() {
    if (this.measureIds == null || this.measureIds.size() == 0) {
      logger.error("Measure Ids must be configured");
      throw new IllegalArgumentException("measureIds");
    }

    if (this.reportingPeriodMethod == null) {
      logger.error("Reporting period method must be configured");
      throw new IllegalArgumentException("reportingPeriodMethod");
    }

    logger.info("Starting scheduled task to generate a report");
    ReportingPeriodCalculator rpc = new ReportingPeriodCalculator(this.reportingPeriodMethod);

    try {
      GenerateRequest generateRequest = new GenerateRequest();
      generateRequest.setBundleIds(this.measureIds.toArray(new String[0]));
      generateRequest.setPeriodStart(rpc.getStart());
      generateRequest.setPeriodEnd(rpc.getEnd());
      generateRequest.setRegenerate(this.regenerateIfExists);
      reportController.generateReport(null, null, generateRequest);
    } catch (Exception e) {
      logger.error("Error generating reporting", e);
    }
  }
}
