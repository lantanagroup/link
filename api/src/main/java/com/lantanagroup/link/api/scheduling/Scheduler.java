package com.lantanagroup.link.api.scheduling;

import com.lantanagroup.link.config.scheduling.GenerateReportConfig;
import com.lantanagroup.link.config.scheduling.ScheduleConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class Scheduler {
  private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

  @Autowired
  private TaskScheduler taskScheduler;

  @Autowired
  private ScheduleConfig config;

  @Autowired
  private ApplicationContext context;

  @PostConstruct
  public void init() {
    if (StringUtils.isNotEmpty(this.config.getDataRetentionCheckCron())) {
      DataRetentionCheckTask dataRetentionCheckTask = this.context.getBean(DataRetentionCheckTask.class);
      this.taskScheduler.schedule(dataRetentionCheckTask, new CronTrigger(this.config.getDataRetentionCheckCron()));
      logger.info("Scheduled data retention check with CRON \"" + this.config.getDataRetentionCheckCron() + "\"");
    }

    if (StringUtils.isNotEmpty(this.config.getQueryPatientListCron())) {
      QueryPatientListTask queryPatientListTask = this.context.getBean(QueryPatientListTask.class);
      this.taskScheduler.schedule(queryPatientListTask, new CronTrigger(this.config.getQueryPatientListCron()));
      logger.info("Scheduled querying patient list with CRON \"" + this.config.getQueryPatientListCron() + "\"");
    }

    if (this.config.getGenerateAndSubmitReports() != null) {
      for (GenerateReportConfig generateReportConfig : this.config.getGenerateAndSubmitReports()) {
        GenerateAndSubmitReportTask generateAndSubmitReportTask = this.context.getBean(GenerateAndSubmitReportTask.class);
        generateAndSubmitReportTask.setMeasureIds(generateReportConfig.getMeasureIds());
        generateAndSubmitReportTask.setReportingPeriodMethod(generateReportConfig.getReportingPeriodMethod());
        generateAndSubmitReportTask.setRegenerateIfExists(generateReportConfig.getRegenerateIfExists());
        this.taskScheduler.schedule(generateAndSubmitReportTask, new CronTrigger(generateReportConfig.getCron()));
        logger.info("Scheduled report generation for " + String.join(",", generateReportConfig.getMeasureIds()) + " with CRON \"" + generateReportConfig.getCron() + "\"");
      }
    }
  }

  /*
  @Scheduled(cron = "#{scheduleConfig.acquireCensusCron}")
  @Async
  public void scheduleAcquireCensus() {
    logger.info("Starting scheduled task for acquiring census");
  }

   */
}
