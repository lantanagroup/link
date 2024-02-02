package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.Helper;
import com.lantanagroup.link.api.scheduling.Scheduler;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.tenant.Tenant;
import com.lantanagroup.link.db.model.tenant.TenantVendors;
import com.lantanagroup.link.model.SearchTenantResponse;
import com.lantanagroup.link.model.TenantSummary;
import com.lantanagroup.link.model.TenantSummaryResponse;
import com.lantanagroup.link.model.TenantSummarySort;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tenant")
public class TenantController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(TenantController.class);

  @Autowired
  private Scheduler scheduler;

  @Autowired
  private SharedService sharedService;

  @InitBinder
  public void initializeBinder(WebDataBinder binder) {
    binder.setDisallowedFields();
  }

  @GetMapping
  public List<SearchTenantResponse> searchTenants() {
    return this.sharedService.getTenantConfigs().stream()
            .map(t -> new SearchTenantResponse(t.getId(), t.getName(), t.getRetentionPeriod(), t.getCdcOrgId(), t.getVendor(), t.getOtherVendor()))
            .collect(Collectors.toList());
  }

  @GetMapping("summary")
  public TenantSummaryResponse searchTenantSummaries(@RequestParam(required = false) String searchCriteria, @RequestParam(defaultValue = "NAME", required = false) String sort, @RequestParam(defaultValue = "1", required = false) int page, @RequestParam(defaultValue = "true", required = false) boolean sortAscend) {
    // validation
    int itemsPerPage = 5;

    if (page < 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page should be greater than 1");
    }

    int skip = (page - 1) * itemsPerPage;

    // validate sort is one of the enum values
    if (sort != null) {
      try {
        TenantSummarySort.valueOf(sort.trim());
      } catch (IllegalArgumentException e) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort criteria. Valid values are NAME, NHSN_ORG_ID, SUBMISSION_DATE");
      }
    }
    List<TenantSummary> tenants = this.sharedService.getTenantSummary(searchCriteria, TenantSummarySort.valueOf(sort.trim()), sortAscend).stream().collect(Collectors.toList());

    List<TenantSummary> results = tenants.stream().skip(skip).limit(itemsPerPage).collect(Collectors.toList());

    TenantSummaryResponse response = new TenantSummaryResponse();
    response.setTotal(tenants.size());
    response.setTenants(results);

    return response;
  }

  @GetMapping("{tenantId}")
  public Tenant getTenant(@PathVariable String tenantId) {
    Tenant tenant = this.sharedService.getTenantConfig(tenantId);

    if (tenant == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    return tenant;
  }

  private static boolean isIdValid(String id) {
    final String regex = "[^a-zA-Z0-9_-]";

    Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
    return !pattern.matcher(id).find();
  }

  private void validateTenantConfig(Tenant newTenantConfig, Tenant existingTenantConfig) {
    if (StringUtils.isEmpty(newTenantConfig.getId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'id' is required");
    }

    if (!isIdValid(newTenantConfig.getId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Id \"%s\" is not valid. The only special characters that are allowed are dashes (-) and underscores (_).", newTenantConfig.getId()));
    }

    if (StringUtils.isEmpty(newTenantConfig.getConnectionString())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'connectionString' is required");
    }

    if (StringUtils.isEmpty(newTenantConfig.getTimeZoneId())) {
      newTenantConfig.setTimeZoneId("UTC");
    }

    if (StringUtils.isNotEmpty(newTenantConfig.getOtherVendor()) && newTenantConfig.getVendor() != TenantVendors.Other) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "If 'otherVendor' is provided, 'vendor' must be 'Other'");
    }

    try {
      TimeZone timezone = TimeZone.getTimeZone(newTenantConfig.getTimeZoneId());
    }
    catch(Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The provided 'timeZoneId' was not a valid Java.util.TimeZone.ID value.");
    }

    List<Tenant> existingTenants = this.sharedService.getTenantConfigs();

    boolean idAlreadyExists =
            existingTenantConfig == null &&
                    StringUtils.isNotEmpty(newTenantConfig.getId()) &&
                    existingTenants.stream().anyMatch(t -> t.getId().equals(newTenantConfig.getId()));
    if (idAlreadyExists) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Tenant with id \"%s\" already exists", newTenantConfig.getId()));
    }

    // TODO: Check if database is already being used by another tenant?

    if (StringUtils.isEmpty(newTenantConfig.getCdcOrgId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'cdcOrgId' is required");
    }

    if (newTenantConfig.getBundling() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'bundling' is required");
    }

    if (StringUtils.isEmpty(newTenantConfig.getBundling().getName())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'bundling.name' is required");
    }

    if (newTenantConfig.getEvents() != null) {
      this.validateEvents("BeforeMeasureResolution", newTenantConfig.getEvents().getBeforeMeasureResolution());
      this.validateEvents("AfterMeasureResolution", newTenantConfig.getEvents().getAfterMeasureResolution());
      this.validateEvents("OnRegeneration", newTenantConfig.getEvents().getOnRegeneration());
      this.validateEvents("BeforePatientOfInterestLookup", newTenantConfig.getEvents().getBeforePatientOfInterestLookup());
      this.validateEvents("AfterPatientOfInterestLookup", newTenantConfig.getEvents().getAfterPatientOfInterestLookup());
      this.validateEvents("BeforePatientDataQuery", newTenantConfig.getEvents().getBeforePatientDataQuery());
      this.validateEvents("AfterPatientResourceQuery", newTenantConfig.getEvents().getAfterPatientResourceQuery());
      this.validateEvents("AfterPatientDataQuery", newTenantConfig.getEvents().getAfterPatientDataQuery());
      this.validateEvents("AfterApplyConceptMaps", newTenantConfig.getEvents().getAfterApplyConceptMaps());
      this.validateEvents("BeforePatientDataStore", newTenantConfig.getEvents().getBeforePatientDataStore());
      this.validateEvents("AfterPatientDataStore", newTenantConfig.getEvents().getAfterPatientDataStore());
      this.validateEvents("BeforeMeasureEval", newTenantConfig.getEvents().getBeforeMeasureEval());
      this.validateEvents("AfterMeasureEval", newTenantConfig.getEvents().getAfterMeasureEval());
      this.validateEvents("BeforeReportStore", newTenantConfig.getEvents().getBeforeReportStore());
      this.validateEvents("AfterReportStore", newTenantConfig.getEvents().getAfterReportStore());
      this.validateEvents("BeforeBundling", newTenantConfig.getEvents().getBeforeBundling());
      this.validateEvents("AfterBundling", newTenantConfig.getEvents().getAfterBundling());
    }
  }

  private void validateEvents(String eventName, List<String> events) {
    if (events == null) {
      return;
    }

    for (String className : events) {
      try {
        Class.forName(className);
      } catch (ClassNotFoundException e) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Class '%s' for event %s could not be found", className, eventName));
      }
    }
  }

  @PutMapping("/{tenantId}")
  public void updateTenant(@RequestBody Tenant tenant, @PathVariable String tenantId) {
    Tenant existingTenantConfig = this.sharedService.getTenantConfig(tenantId);

    if (existingTenantConfig == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    } else if (!existingTenantConfig.getConnectionString().equals(tenant.getConnectionString())) {
      String newDatabaseName = Helper.getDatabaseName(tenant.getConnectionString());
      List<String> allDatabaseNames = this.sharedService.getAllDatabaseNames(tenantId);

      if (allDatabaseNames.contains(newDatabaseName)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Database connection string uses the same database name as another");
      }

      TenantService tenantService = TenantService.create(tenant);

      try {
        tenantService.testConnection();
      } catch (SQLException e) {
        logger.error("Failed to test connection to new tenant database", e);
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not connect to database specified for tenant. Updates to tenant not persisted.");
      }

      if (!this.config.isApplySchemas()) {
        logger.warn("Not configured to apply schemas to database. Skipping tenant database schema init.");
      } else {
        tenantService.initDatabase();
      }
    }

    this.validateTenantConfig(tenant, existingTenantConfig);

    tenant.setId(tenantId);
    this.sharedService.saveTenantConfig(tenant);
    this.scheduler.reset(tenantId);
  }

  @PostMapping
  public Tenant createTenant(@RequestBody Tenant tenant) {
    this.validateTenantConfig(tenant, null);

    String newDatabaseName = Helper.getDatabaseName(tenant.getConnectionString());
    List<String> allDatabaseNames = this.sharedService.getAllDatabaseNames(null);

    if (allDatabaseNames.contains(newDatabaseName)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Database connection string uses the same database name as another");
    }

    TenantService tenantService = TenantService.create(tenant);

    try {
      tenantService.testConnection();
    } catch (SQLException e) {
      logger.error("Failed to test connection to new tenant database", e);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not connect to database specified for new tenant. Tenant not created.");
    }

    if (!this.config.isApplySchemas()) {
      logger.warn("Not configured to apply schemas to database. Skipping tenant database schema init.");
    } else {
      tenantService.initDatabase();
    }

    this.sharedService.saveTenantConfig(tenant);
    this.scheduler.reset(tenant.getId());

    return tenant;
  }

  @DeleteMapping("/{tenantId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteTenant(@PathVariable String tenantId) {
    if (this.sharedService.deleteTenantConfig(tenantId) == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    // Update the scheduling system to remove the tenant's schedules
    this.scheduler.reset(tenantId);
  }
}
