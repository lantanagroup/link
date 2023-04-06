package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.api.scheduling.Scheduler;
import com.lantanagroup.link.db.MongoService;
import com.lantanagroup.link.db.model.TenantConfig;
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
  private MongoService mongoService;

  @GetMapping
  public List<TenantConfig> searchTenants() {
    return this.mongoService.searchTenantConfigs();
  }

  @GetMapping("{tenantId}")
  public TenantConfig getTenant(@PathVariable String tenantId) {
    TenantConfig tenantConfig = this.mongoService.getTenantConfig(tenantId);

    if (tenantConfig == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    return tenantConfig;
  }

  private void validateTenantConfig(TenantConfig tenantConfig) {
    // TODO
  }

  @PutMapping("/{tenantId}")
  public void updateTenant(@RequestBody TenantConfig tenantConfig, @PathVariable String tenantId) {
    if (this.mongoService.getTenantConfig(tenantId) == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    this.validateTenantConfig(tenantConfig);

    tenantConfig.setId(tenantId);
    this.mongoService.saveTenantConfig(tenantConfig);
    this.scheduler.reset(tenantId);
  }

  @PostMapping
  public TenantConfig createTenant(@RequestBody TenantConfig tenantConfig) {
    this.validateTenantConfig(tenantConfig);

    if (StringUtils.isNotEmpty(tenantConfig.getId()) && this.mongoService.getTenantConfig(tenantConfig.getId()) != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant already exists. Did you mean to PUT?");
    } else if (StringUtils.isEmpty(tenantConfig.getId())) {
      tenantConfig.setId((new ObjectId()).toString());
    }

    this.mongoService.saveTenantConfig(tenantConfig);
    this.scheduler.reset(tenantConfig.getId());

    return tenantConfig;
  }

  @DeleteMapping("/{tenantId}")
  public void deleteTenant(@PathVariable String tenantId) {
    if (this.mongoService.deleteTenantConfig(tenantId) == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    this.scheduler.reset(tenantId);
  }
}
