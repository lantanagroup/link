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
    if (StringUtils.isNotEmpty(this.config.getAcquireCensusCron())) {
      AcquireCensusTask acquireCensusTask = this.context.getBean(AcquireCensusTask.class);
      this.taskScheduler.schedule(acquireCensusTask, new CronTrigger(this.config.getAcquireCensusCron()));
      logger.info("Scheduled census acquisition with CRON \"" + this.config.getAcquireCensusCron() + "\"");
    }

    if (this.config.getGenerateReport() != null) {
      for (GenerateReportConfig generateReportConfig : this.config.getGenerateReport()) {
        GenerateReportTask generateReportTask = this.context.getBean(GenerateReportTask.class);
        generateReportTask.setMeasureIds(generateReportConfig.getMeasureIds());
        generateReportTask.setReportingPeriodMethod(generateReportConfig.getReportingPeriodMethod());
        this.taskScheduler.schedule(generateReportTask, new CronTrigger(generateReportConfig.getCron()));
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
