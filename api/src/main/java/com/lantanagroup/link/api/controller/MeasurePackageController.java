package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.model.MeasureDefinition;
import com.lantanagroup.link.db.model.MeasurePackage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/measurePackage")
public class MeasurePackageController extends BaseController {
  private static final Logger logger = LoggerFactory.getLogger(MeasurePackageController.class);

  @Autowired
  private SharedService sharedService;

  @PutMapping
  public void createOrUpdateMeasurePackage(@RequestBody(required = false) MeasurePackage measurePackage) {
    if (StringUtils.isEmpty(measurePackage.getId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Measure package must specify an id");
    }

    if (measurePackage.getMeasureIds() == null || measurePackage.getMeasureIds().size() == 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Measure package must specify one or more measure ids");
    }

    List<MeasureDefinition> measureDefinitions = this.sharedService.getMeasureDefinitions();

    List<String> notFoundMeasureIds = measurePackage.getMeasureIds().stream()
            .filter(measureId ->
                    measureDefinitions.stream().noneMatch(md -> md.getMeasureId().equals(measureId)))
            .collect(Collectors.toList());

    if (notFoundMeasureIds.size() > 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Could not find measure definitions for the following ids: %s", String.join(", ", notFoundMeasureIds)));
    }

    this.sharedService.saveMeasurePackage(measurePackage);
  }

  @GetMapping
  public List<MeasurePackage> searchMeasurePackages() {
    return this.sharedService.getMeasurePackages();
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteMeasurePackage(@PathVariable String id) {
    Optional<MeasurePackage> measurePackage = this.sharedService.getMeasurePackages()
            .stream().filter(mp -> mp.getId().equals(id))
            .findFirst();

    if (measurePackage.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Measure package %s not found", id));
    }

    this.sharedService.deleteMeasurePackage(id);
  }
}
