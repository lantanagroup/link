package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.api.scheduling.Scheduler;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.model.tenant.Tenant;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/tenant")
public class TenantController extends BaseController {
  @Autowired
  private Scheduler scheduler;

  @Autowired
  private SharedService sharedService;

  @GetMapping
  public List<Tenant> searchTenants() {
    return this.sharedService.searchTenantConfigs();
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
    if (!isIdValid(newTenantConfig.getId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Id \"%s\" is not valid. The only special characters that are allowed are dashes (-) and underscores (_).", newTenantConfig.getId()));
    }

    if (!isIdValid(newTenantConfig.getDatabase())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Database name \"%s\" is not valid. The only special characters that are allowed are dashes (-) and underscores (_).", newTenantConfig.getDatabase()));
    }

    List<Tenant> existingTenants = this.sharedService.searchTenantConfigs();

    boolean idAlreadyExists =
            existingTenantConfig == null &&
                    StringUtils.isNotEmpty(newTenantConfig.getId()) &&
                    existingTenants.stream().anyMatch(t -> t.getId().equals(newTenantConfig.getId()));
    boolean databaseAlreadyExists =
            existingTenants.stream().anyMatch(t ->
                    !t.getId().equals(newTenantConfig.getId()) &&
                            t.getDatabase().equalsIgnoreCase(newTenantConfig.getDatabase()));

    if (idAlreadyExists) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Tenant with id \"%s\" already exists", newTenantConfig.getId()));
    }

    if (StringUtils.isEmpty(newTenantConfig.getDatabase())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant must specify a database name");
    } else if (existingTenantConfig != null && !existingTenantConfig.getDatabase().equals(newTenantConfig.getDatabase())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("The database name cannot be changed from \"%s\" to \"%s\"", existingTenantConfig.getDatabase(), newTenantConfig.getDatabase()));
    }

    if (databaseAlreadyExists) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Another tenant is using the database name %s", newTenantConfig.getDatabase()));
    }

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
    }

    this.validateTenantConfig(tenant, existingTenantConfig);

    tenant.setId(tenantId);
    this.sharedService.saveTenantConfig(tenant);
    this.scheduler.reset(tenantId);
  }

  @PostMapping
  public Tenant createTenant(@RequestBody Tenant tenant) {
    if (StringUtils.isEmpty(tenant.getId())) {
      tenant.setId((new ObjectId()).toString());
    }

    this.validateTenantConfig(tenant, null);

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
