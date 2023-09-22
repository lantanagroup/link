package com.lantanagroup.link.api.scheduling;

import com.lantanagroup.link.ReportingPeriodCalculator;
import com.lantanagroup.link.ReportingPeriodMethods;
import com.lantanagroup.link.api.controller.ReportController;
import com.lantanagroup.link.db.model.Report;
import com.lantanagroup.link.model.GenerateRequest;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GenerateAndSubmitReportTask implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(GenerateAndSubmitReportTask.class);

  @Setter
  private List<String> measureIds;

  @Setter
  private ReportingPeriodMethods reportingPeriodMethod;

  @Setter
  private Boolean regenerateIfExists = false;

  @Setter
  private String tenantId;

  @Autowired
  private ReportController reportController;

  @Override
  public void run() {
    if (this.measureIds == null || this.measureIds.isEmpty()) {
      logger.error("Measure Ids must be configured");
      throw new IllegalArgumentException("measureIds");
    }

    if (this.reportingPeriodMethod == null) {
      logger.error("Reporting period method must be configured");
      throw new IllegalArgumentException("reportingPeriodMethod");
    }

    logger.info("Starting scheduled task to generate a report");
    ReportingPeriodCalculator rpc = new ReportingPeriodCalculator(this.reportingPeriodMethod);
    Report report;

    try {
      GenerateRequest generateRequest = new GenerateRequest();
      generateRequest.setBundleIds(this.measureIds);
      generateRequest.setPeriodStart(rpc.getStart());
      generateRequest.setPeriodEnd(rpc.getEnd());
      generateRequest.setRegenerate(this.regenerateIfExists);

      logger.info("Scheduled task generating report for measure(s) {} with start {} and end {} from reporting period method {}", String.join("|", this.measureIds), rpc.getStart(), rpc.getEnd(), this.reportingPeriodMethod);
      report = this.reportController.generateReport(null, null, this.tenantId, generateRequest);
    } catch (Exception e) {
      logger.error("Error generating report", e);
      return;
    }

    try {
      this.reportController.send(null, report.getId(), this.tenantId, false, null);
    } catch (Exception ex) {
      logger.error("Error submitting report", ex);
    }

    logger.info("Done executing generate and submit report scheduled task");
  }
}
