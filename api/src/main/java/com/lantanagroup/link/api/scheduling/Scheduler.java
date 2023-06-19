package com.lantanagroup.link.api.scheduling;

import com.lantanagroup.link.api.bulk.BulkStatusFetchTask;
import com.lantanagroup.link.api.bulk.InitiateBulkDataRequestTask;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.model.tenant.GenerateReport;
import com.lantanagroup.link.db.model.tenant.Schedule;
import com.lantanagroup.link.db.model.tenant.Tenant;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * Automatically invoked by spring boot during application start up. Responsible for reading the config files and
 * registering scheduled tasks with the TaskScheduler.
 */
@Component
public class Scheduler {
  private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

  @Autowired
  private TaskScheduler taskScheduler;

  @Autowired
  private ApplicationContext context;

  @Autowired
  private SharedService sharedService;

  private Dictionary<String, List<ScheduledFuture>> schedules = new Hashtable<>();

  public void reset(String tenantId) {
    List<ScheduledFuture> tenantSchedules = this.schedules.get(tenantId);

    if (tenantSchedules != null && tenantSchedules.size() > 0) {
      logger.info("Removing {} currently scheduled tasks", tenantSchedules.size());
      tenantSchedules.forEach(s -> s.cancel(false));
      this.schedules.remove(tenantId);
    }

    Tenant tenant = this.sharedService.getTenantConfig(tenantId);

    if (tenant == null) {
      logger.warn("Tenant {} no longer exists, not going to re-initialize schedules for tenant", tenantId);
    } else if (tenant.getScheduling() != null) {
      logger.info("Re-initializing scheduled tasks for tenant {}", tenantId);
      this.init(tenant.getId(), tenant.getScheduling());
    }
  }

  private void addFuture(String tenantId, ScheduledFuture scheduledFuture) {
    List<ScheduledFuture> tenantSchedules = this.schedules.get(tenantId);
    if (tenantSchedules == null) {
      tenantSchedules = new ArrayList<>();
      this.schedules.put(tenantId, tenantSchedules);
    }
    tenantSchedules.add(scheduledFuture);
  }

  private void init(String tenantId, Schedule config) {
    // TODO: Make sure this.context.getBean() doesn't return a singleton

    if (StringUtils.isNotEmpty(config.getDataRetentionCheckCron())) {
      DataRetentionCheckTask dataRetentionCheckTask = this.context.getBean(DataRetentionCheckTask.class);
      dataRetentionCheckTask.setTenantId(tenantId);
      ScheduledFuture dataRetentionFuture = this.taskScheduler.schedule(dataRetentionCheckTask, new CronTrigger(config.getDataRetentionCheckCron()));
      this.addFuture(tenantId, dataRetentionFuture);
      logger.info("Scheduled data retention check for tenant {} with CRON \"{}\"", tenantId, config.getDataRetentionCheckCron());
    }

    if (StringUtils.isNotEmpty(config.getQueryPatientListCron())) {
      QueryPatientListTask queryPatientListTask = this.context.getBean(QueryPatientListTask.class);
      queryPatientListTask.setTenantId(tenantId);
      ScheduledFuture queryPatientListFuture = this.taskScheduler.schedule(queryPatientListTask, new CronTrigger(config.getQueryPatientListCron()));
      this.addFuture(tenantId, queryPatientListFuture);
      logger.info("Scheduled querying patient list for tenant {} with CRON \"{}\"", tenantId, config.getQueryPatientListCron());
    }

    if (config.getGenerateAndSubmitReports() != null) {
      for (GenerateReport generateReportConfig : config.getGenerateAndSubmitReports()) {
        GenerateAndSubmitReportTask generateAndSubmitReportTask = this.context.getBean(GenerateAndSubmitReportTask.class);
        generateAndSubmitReportTask.setTenantId(tenantId);
        generateAndSubmitReportTask.setMeasureIds(generateReportConfig.getMeasureIds());
        generateAndSubmitReportTask.setReportingPeriodMethod(generateReportConfig.getReportingPeriodMethod());
        generateAndSubmitReportTask.setRegenerateIfExists(generateReportConfig.getRegenerateIfExists());
        ScheduledFuture generateAndSubmitReportFuture = this.taskScheduler.schedule(generateAndSubmitReportTask, new CronTrigger(generateReportConfig.getCron()));
        this.addFuture(tenantId, generateAndSubmitReportFuture);
        logger.info("Scheduled report generation for tenant {} for measures {} with CRON \"{}\"", tenantId, String.join(",", generateReportConfig.getMeasureIds()), generateReportConfig.getCron());
      }
    }

    if(StringUtils.isNotEmpty(config.getBulkDataCron())){
      InitiateBulkDataRequestTask bulkDataTask = this.context.getBean(InitiateBulkDataRequestTask.class);
      bulkDataTask.setTenantId(tenantId);
      ScheduledFuture bulkDataFuture = this.taskScheduler.schedule(bulkDataTask, new CronTrigger(config.getBulkDataCron()));
      this.addFuture(tenantId, bulkDataFuture);
      logger.info("Scheduled bulk data initiate for tenant {} with CRON \"{}\"", tenantId, config.getBulkDataCron());

      BulkStatusFetchTask fetchTask = this.context.getBean(BulkStatusFetchTask.class);
      fetchTask.setTenantId(tenantId);
      ScheduledFuture bulkFetchFuture = this.taskScheduler.schedule(fetchTask, new CronTrigger(config.getBulkDataFollowUpCron()));
      this.addFuture(tenantId, bulkFetchFuture);
      logger.info("Scheduled bulk data status fetch for tenant {} with CRON \"{}\"", tenantId, config.getBulkDataFollowUpCron());
    }
  }

  @PostConstruct
  public void init() {
    List<Tenant> tenants = this.sharedService.getTenantSchedules();

    // Loop through each of the tenants and register their scheduled tasks
    for (Tenant tenant : tenants) {
      if (tenant.getScheduling() == null) {
        logger.warn("No scheduled tasks setup for tenant {}", tenant.getId());
        continue;
      }

      this.init(tenant.getId(), tenant.getScheduling());
    }
  }
}
