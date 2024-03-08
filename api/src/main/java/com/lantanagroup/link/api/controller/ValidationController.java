package com.lantanagroup.link.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.ValidationCategorizer;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.mappers.ValidationResultMapper;
import com.lantanagroup.link.db.model.Report;
import com.lantanagroup.link.db.model.tenant.ValidationResult;
import com.lantanagroup.link.db.model.tenant.ValidationResultCategory;
import com.lantanagroup.link.model.*;
import com.lantanagroup.link.time.StopwatchManager;
import com.lantanagroup.link.validation.RuleBasedValidationCategory;
import com.lantanagroup.link.validation.ValidationService;
import com.lantanagroup.link.validation.Validator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/validate")
public class ValidationController extends BaseController {

  private static final Logger logger = LoggerFactory.getLogger(ValidationController.class);
  @Autowired
  private Validator validator;
  @Autowired
  private SharedService sharedService;
  @Autowired
  private ValidationService validationService;
  @Autowired
  private ApiConfig apiConfig;

  /**
   * Validates a Bundle provided in the request body
   *
   * @param tenantId The id of the tenant
   * @param severity The minimum severity level to report on
   * @return Returns an OperationOutcome resource that provides details about each of the issues found
   */
  @PostMapping
  public OperationOutcome validate(@RequestBody Bundle bundle, @RequestParam(defaultValue = "INFORMATION") OperationOutcome.IssueSeverity severity) {
    OperationOutcome outcome = this.validator.validate(bundle, severity);

    Device found = bundle.getEntry().stream()
            .filter(e -> e.getResource() instanceof Device)
            .map(e -> (Device) e.getResource())
            .findFirst()
            .orElse(null);

    if (found != null) {
      outcome.addContained(found);
    }

    return outcome;
  }

  /**
   * Validates a Bundle provided in the request body
   *
   * @param tenantId The id of the tenant
   * @param severity The minimum severity level to report on
   * @return Returns an OperationOutcome resource that provides details about each of the issues found
   */
  @PostMapping("/summary")
  public String validateSummary(@RequestBody Bundle bundle, @RequestParam(defaultValue = "INFORMATION") OperationOutcome.IssueSeverity severity) {
    OperationOutcome outcome = this.validator.validate(bundle, severity);
    return this.getValidationSummary(outcome);
  }

  /**
   * Gets the validation results for a stored report
   * @param tenantId The id of the tenant
   * @param reportId The id of the report to validate against
   * @param severity The minimum severity level to report on
   * @return Returns an OperationOutcome resource that provides details about each of the issues found
   * @throws IOException
   */
  @GetMapping("/{tenantId}/{reportId}")
  public OperationOutcome getValidationIssuesForReport(@PathVariable String tenantId, @PathVariable String reportId, @RequestParam(defaultValue = "INFORMATION") OperationOutcome.IssueSeverity severity, @RequestParam(required = false) String code) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);

    if (tenantService == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    Report report = tenantService.getReport(reportId);

    if (report == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found");
    }

    OperationOutcome outcome = tenantService.getValidationResultsOperationOutcome(reportId, severity, code);

    if (report.getDeviceInfo() != null) {
      outcome.addContained(report.getDeviceInfo());
    }

    return outcome;
  }

  /**
   * Provides a summary of unique messages from validation results
   * @param tenantId The id of the tenant
   * @param reportId The id of the report to validate against
   * @param severity The minimum severity level to report on
   * @return Returns a plain string, each line representing a single message (including severity)
   * @throws IOException
   */
  @GetMapping("/{tenantId}/{reportId}/summary")
  public String getValidationSummary(@PathVariable String tenantId, @PathVariable String reportId, @RequestParam(defaultValue = "INFORMATION") OperationOutcome.IssueSeverity severity, @RequestParam(required = false) String code) {
    OperationOutcome outcome = this.getValidationIssuesForReport(tenantId, reportId, severity, code);
    return this.getValidationSummary(outcome);
  }

  private static ValidationCategoryResponse buildUncategorizedCategory(int count) {
    ValidationCategoryResponse response = new ValidationCategoryResponse();
    response.setId("uncategorized");
    response.setTitle("Uncategorized");
    response.setSeverity(ValidationCategorySeverities.WARNING);
    response.setAcceptable(false);
    response.setGuidance("These issues need to be categorized.");
    response.setCount(count);
    return response;
  }

  /**
   * Retrieves the validation categories for a report, for a custom set of categories
   *
   * @param tenantId The id of the tenant
   * @param reportId The id of the report to validate against
   * @return Returns a list of ValidationCategoryResponse objects
   * @throws JsonProcessingException
   */
  @PostMapping("/{tenantId}/{reportId}/category")
  public List<ValidationCategoryResponse> getValidationForCustomCategories(@PathVariable String tenantId, @PathVariable String reportId, @RequestBody List<RuleBasedValidationCategory> categories) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);

    if (tenantService == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    Report report = tenantService.getReport(reportId);

    if (report == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found");
    }

    List<ValidationResult> results = tenantService.getValidationResults(reportId);
    ValidationCategorizer categorizer = new ValidationCategorizer();
    categorizer.setCategories(categories);
    List<ValidationResultCategory> categorizedResults = categorizer.categorize(results);
    List<ValidationResult> uncategorizedResults = results.stream().filter(r -> {
      return categorizedResults.stream().noneMatch(cr -> cr.getValidationResultId().equals(r.getId()));
    }).collect(Collectors.toList());

    List<ValidationCategoryResponse> responses = categories.stream()
            .map(c -> {
              ValidationCategoryResponse response = new ValidationCategoryResponse(c);
              response.setCount(categorizedResults.stream().filter(rc -> rc.getCategoryCode().equals(c.getId())).count());
              return response;
            })
            .filter(c -> c.getCount() > 0)
            .collect(Collectors.toList());

    if (!uncategorizedResults.isEmpty()) {
      responses.add(buildUncategorizedCategory(uncategorizedResults.size()));
    }

    return responses;
  }

  /**
   * Retrieves the validation results for a report, for a custom set of categories, for a specific category. This is used to test that construction of validation categorizes against a given report and see how well the categories work against the validation results before committing the validation categories.
   *
   * @param tenantId   The id of the tenant
   * @param reportId   The id of the report to validate against
   * @param categoryId The id of the category to retrieve results for
   * @return Returns a list of ValidationCategoryResponse objects
   * @throws JsonProcessingException
   */
  @PostMapping("/{tenantId}/{reportId}/category/{categoryId}")
  public OperationOutcome getValidationForCustomCategory(@PathVariable String tenantId, @PathVariable String reportId, @PathVariable String categoryId, @RequestBody List<RuleBasedValidationCategory> categories) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);

    if (tenantService == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    Report report = tenantService.getReport(reportId);

    if (report == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found");
    }

    List<ValidationResult> results = tenantService.getValidationResults(reportId);
    ValidationCategorizer categorizer = new ValidationCategorizer();
    categorizer.setCategories(categories);
    List<ValidationResultCategory> categorizedResults = categorizer.categorize(results);
    List<ValidationResult> uncategorizedResults = results.stream().filter(r -> {
      return categorizedResults.stream().noneMatch(cr -> cr.getValidationResultId().equals(r.getId()));
    }).collect(Collectors.toList());

    if (categoryId.equals("uncategorized")) {
      return ValidationResultMapper.toOperationOutcome(uncategorizedResults);
    }

    List<ValidationResult> resultsForCategory = categorizedResults.stream()
            .filter(cr -> cr.getCategoryCode().equals(categoryId))
            .map(cr -> results.stream().filter(r -> r.getId().equals(cr.getValidationResultId())).findFirst().orElse(null))
            .filter(r -> r != null)
            .collect(Collectors.toList());

    return ValidationResultMapper.toOperationOutcome(resultsForCategory);
  }

  /**
   * Retrieves the validation categories for a report
   * @param tenantId The id of the tenant
   * @param reportId The id of the report to validate against
   * @return Returns a list of ValidationCategoryResponse objects
   * @throws JsonProcessingException
   */
  @GetMapping("/{tenantId}/{reportId}/category")
  public List<ValidationCategoryResponse> getValidationCategories(@PathVariable String tenantId, @PathVariable String reportId) throws JsonProcessingException {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);

    if (tenantService == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    Report report = tenantService.getReport(reportId);

    if (report == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found");
    }

    List<ValidationCategory> categories = ValidationCategorizer.loadAndRetrieveCategories();
    List<ValidationResultCategory> resultCategories = tenantService.findValidationResultCategoriesByReport(reportId);
    Integer uncategorizedCount = tenantService.countUncategorizedValidationResults(reportId);

    List<ValidationCategoryResponse> responses = categories.stream()
            .map(c -> {
              ValidationCategoryResponse response = new ValidationCategoryResponse(c);
              response.setCount(resultCategories.stream().filter(rc -> rc.getCategoryCode().equals(c.getId())).count());
              return response;
            })
            .filter(c -> c.getCount() > 0)
            .collect(Collectors.toList());

    if (uncategorizedCount > 0) {
      responses.add(buildUncategorizedCategory(uncategorizedCount));
    }

    return responses;
  }

  /**
   * Retrieves the validation results for a report that are not categorized
   * @param tenantId The id of the tenant
   * @param reportId The id of the report to validate against
   * @return Returns an OperationOutcome resource that provides details about each of the issues found
   */
  @GetMapping("/{tenantId}/{reportId}/category/uncategorized")
  public OperationOutcome getUncategorizedValidationResults(@PathVariable String tenantId, @PathVariable String reportId) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);

    if (tenantService == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    Report report = tenantService.getReport(reportId);

    if (report == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found");
    }

    List<ValidationResult> uncategorizedResults = tenantService.getUncategorizedValidationResults(reportId);

    if (uncategorizedResults.isEmpty()) {
      return ValidationResultMapper.toOperationOutcome(null);
    }

    return ValidationResultMapper.toOperationOutcome(uncategorizedResults);
  }

  /**
   * Re-validates a generated report and persists the validation results. Always validates at INFORMATION severity level.
   * @param tenantId The id of the tenant
   * @param reportId The id of the report to validate against
   * @return Returns an OperationOutcome resource that provides details about each of the issues found
   * @throws IOException
   */
  @PostMapping("/{tenantId}/{reportId}")
  public OperationOutcome validate(@PathVariable String tenantId, @PathVariable String reportId) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);

    if (tenantService == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    Report report = tenantService.getReport(reportId);

    if (report == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found");
    }

    StopwatchManager stopwatchManager = new StopwatchManager(this.sharedService);
    OperationOutcome outcome = this.validationService.validate(stopwatchManager, tenantService, report);
    stopwatchManager.storeMetrics(tenantId, reportId);

    if (report.getDeviceInfo() != null) {
      outcome.addContained(report.getDeviceInfo());
    }

    // Update the report's device with the current list of implementation guides
    Device device = report.getDeviceInfo();
    if (device == null) {
      device = FhirHelper.getDevice(this.apiConfig);
    }
    FhirHelper.setImplementationGuideNotes(device, this.validator);
    tenantService.saveReport(report);

    return outcome;
  }

  private String getValidationSummary(OperationOutcome outcome) {
    List<String> uniqueMessages = new ArrayList<>();

    for (OperationOutcome.OperationOutcomeIssueComponent issue : outcome.getIssue()) {
      String message = issue.getSeverity().toString() + ": " + issue.getDetails().getText();
      if (!uniqueMessages.contains(message)) {
        uniqueMessages.add(message);
      }
    }

    if (!uniqueMessages.isEmpty()) {
      return "* " + String.join("\n* ", uniqueMessages);
    }

    return "No issues found";
  }

  /** Retrieves the validation results for a report that are categorized
   * @param tenantId The id of the tenant
   * @param reportId The id of the report to validate against
   * @param categoryId The id of the category to retrieve results for
   * @return Returns an OperationOutcome resource that provides details about each of the issues found
   */
  @GetMapping("/{tenantId}/{reportId}/category/{categoryId}")
  public OperationOutcome getValidationCategoryResults(@PathVariable String tenantId, @PathVariable String reportId, @PathVariable String categoryId) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);

    if (tenantService == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    Report report = tenantService.getReport(reportId);

    if (report == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found");
    }

    List<ValidationResult> categoryResults = tenantService.findValidationResultsByCategory(reportId, categoryId);

    if (categoryResults.isEmpty()) {
      return ValidationResultMapper.toOperationOutcome(null);
    }

    return ValidationResultMapper.toOperationOutcome(categoryResults);
  }

  /**
   * Retrieves the validation categories and results for a report
   *
   * @param tenantId The id of the tenant
   * @param reportId The id of the report to validate against
   * @return Returns a ValidationCategoriesAndResults object
   */
  @GetMapping(value = "/{tenantId}/{reportId}/category/result", produces = {"application/xml", "application/json"})
  public ValidationCategoriesAndResults getValidationCategoriesAndResults(@PathVariable String tenantId, @PathVariable String reportId) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);

    if (tenantService == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    Report report = tenantService.getReport(reportId);

    if (report == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found");
    }

    ValidationCategoriesAndResults categoriesAndResults = new ValidationCategoriesAndResults();
    List<ValidationCategory> categories = ValidationCategorizer.loadAndRetrieveCategories();
    List<ValidationResult> results = tenantService.getValidationResults(reportId);
    List<ValidationResultCategory> resultCategories = tenantService.findValidationResultCategoriesByReport(reportId);

    categoriesAndResults.setCategories(categories.stream()
            .map(c -> {
              ValidationCategoryResponse response = new ValidationCategoryResponse(c);
              response.setCount(resultCategories.stream().filter(rc -> rc.getCategoryCode().equals(c.getId())).count());
              return response;
            })
            .filter(c -> c.getCount() > 0)
            .collect(Collectors.toList()));

    categoriesAndResults.setResults(results.stream().map(r -> {
      ValidationResultResponse response = new ValidationResultResponse();
      response.setId(r.getId());
      response.setCode(r.getCode());
      response.setDetails(r.getDetails());
      response.setSeverity(r.getSeverity());
      response.setExpression(r.getExpression());
      response.setPosition(r.getPosition());
      response.setCategories(resultCategories.stream()
              .filter(rc -> rc.getValidationResultId().equals(r.getId()))
              .map(ValidationResultCategory::getCategoryCode)
              .collect(Collectors.toList()));
      return response;
    }).collect(Collectors.toList()));

    return categoriesAndResults;
  }
}
