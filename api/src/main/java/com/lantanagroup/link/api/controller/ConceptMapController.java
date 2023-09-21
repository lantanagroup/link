package com.lantanagroup.link.api.controller;


import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.ConceptMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/{tenantId}/conceptMap")
public class ConceptMapController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(ConceptMapController.class);

  @Autowired
  private SharedService sharedService;

  @InitBinder
  public void initializeBinder(WebDataBinder binder) {
    binder.setDisallowedFields();
  }

  @PutMapping
  public void createOrUpdateConceptMap(@RequestBody ConceptMap conceptMap, @PathVariable String tenantId) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);

    if (tenantService == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    if (StringUtils.isEmpty(conceptMap.getId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Concept map ID is required");
    }

    if (conceptMap.getConceptMap() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The conceptMap FHIR resource is required");
    }

    logger.debug("Saving concept map {}", conceptMap.getId());

    tenantService.saveConceptMap(conceptMap);
  }

  @GetMapping
  public List<ConceptMap> searchConceptMap(@PathVariable String tenantId) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);

    if (tenantService == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    logger.debug("Searching concept maps for tenant {}", tenantId);

    return tenantService.searchConceptMaps();
  }

  @GetMapping("/{id}")
  public ConceptMap getConceptMap(@PathVariable String id, @PathVariable String tenantId) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);

    if (tenantService == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    logger.debug("Retrieving concept map ID {} for tenant {}", id, tenantId);

    return tenantService.getConceptMap(id);
  }
}
