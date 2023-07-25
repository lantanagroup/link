package com.lantanagroup.link.api.controller;


import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.ConceptMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/{tenantId}/conceptMap")
public class ConceptMapController extends BaseController {
  @Autowired
  private SharedService sharedService;

  @InitBinder
  public void initializeBinder(WebDataBinder binder) {
    binder.setDisallowedFields();
  }

  @PutMapping
  public void createOrUpdateConceptMap(@RequestBody ConceptMap conceptMap, @PathVariable String tenantId) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);
    tenantService.saveConceptMap(conceptMap);
  }

  @GetMapping
  public List<ConceptMap> searchConceptMap(@PathVariable String tenantId) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);
    return tenantService.searchConceptMaps();
  }

  @GetMapping("/{id}")
  public ConceptMap getConceptMap(@PathVariable String id, @PathVariable String tenantId) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);
    return tenantService.getConceptMap(id);
  }
}
