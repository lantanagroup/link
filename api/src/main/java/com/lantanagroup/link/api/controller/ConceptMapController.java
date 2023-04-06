package com.lantanagroup.link.api.controller;


import com.lantanagroup.link.TenantService;
import com.lantanagroup.link.db.model.ConceptMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/{tenantId}/conceptMap")
public class ConceptMapController extends BaseController {
  @Autowired
  private TenantService tenantService;

  @PutMapping
  public void createOrUpdateConceptMap(@RequestBody org.hl7.fhir.r4.model.ConceptMap conceptMap) {
    if (!conceptMap.hasId()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ConceptMap must have an id");
    }

    ConceptMap dbConceptMap = new ConceptMap();
    dbConceptMap.setId(conceptMap.getIdElement().getIdPart());
    dbConceptMap.setName(conceptMap.getName());
    dbConceptMap.setResource(conceptMap);

    this.tenantService.saveConceptMap(dbConceptMap);
  }

  @GetMapping
  public List<org.hl7.fhir.r4.model.ConceptMap> searchConceptMap() {
    return this.tenantService.getAllConceptMaps()
            .stream()
            .map(dbConceptMap -> dbConceptMap.getResource())
            .collect(Collectors.toList());
  }
}
