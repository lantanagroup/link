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

  private void validateTenantConfig(Tenant tenant) {

    // TODO
  }

  @PutMapping("/{tenantId}")
  public void updateTenant(@RequestBody Tenant tenant, @PathVariable String tenantId) {
    if (this.sharedService.getTenantConfig(tenantId) == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    this.validateTenantConfig(tenant);

    tenant.setId(tenantId);
    this.sharedService.saveTenantConfig(tenant);
    this.scheduler.reset(tenantId);
  }

  @PostMapping
  public Tenant createTenant(@RequestBody Tenant tenant) {
    this.validateTenantConfig(tenant);

    if (StringUtils.isNotEmpty(tenant.getId()) && this.sharedService.getTenantConfig(tenant.getId()) != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant already exists. Did you mean to PUT?");
    } else if (StringUtils.isEmpty(tenant.getId())) {
      tenant.setId((new ObjectId()).toString());
    }

    this.sharedService.saveTenantConfig(tenant);
    this.scheduler.reset(tenant.getId());

    return tenant;
  }

  @DeleteMapping("/{tenantId}")
  public void deleteTenant(@PathVariable String tenantId) {
    if (this.sharedService.deleteTenantConfig(tenantId) == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    // TODO: Remove concept maps for the tenant

    this.scheduler.reset(tenantId);
  }
}
