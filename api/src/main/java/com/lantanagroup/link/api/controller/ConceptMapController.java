package com.lantanagroup.link.api.controller;


import com.lantanagroup.link.TenantService;
import com.lantanagroup.link.db.MongoService;
import com.lantanagroup.link.db.model.ConceptMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/{tenantId}/conceptMap")
public class ConceptMapController extends BaseController {
  @Autowired
  private MongoService mongoService;

  @PutMapping
  public void createOrUpdateConceptMap(@RequestBody ConceptMap conceptMap, @PathVariable String tenantId) {
    TenantService tenantService = TenantService.create(this.mongoService, tenantId);
    tenantService.saveConceptMap(conceptMap);
  }

  @GetMapping
  public List<ConceptMap> searchConceptMap(@PathVariable String tenantId) {
    TenantService tenantService = TenantService.create(this.mongoService, tenantId);
    return tenantService.searchConceptMaps();
  }

  @GetMapping("/{id}")
  public ConceptMap getConceptMap(@PathVariable String id, @PathVariable String tenantId) {
    TenantService tenantService = TenantService.create(this.mongoService, tenantId);
    return tenantService.getConceptMap(id);
  }
}
