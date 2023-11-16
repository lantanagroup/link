package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.EventService;
import com.lantanagroup.link.Helper;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.Report;
import com.lantanagroup.link.time.Stopwatch;
import com.lantanagroup.link.time.StopwatchManager;
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

@RestController
@RequestMapping("/api/validate")
public class ValidationController extends BaseController {

  private static final Logger logger = LoggerFactory.getLogger(ValidationController.class);
  @Autowired
  private Validator validator;
  @Autowired
  private ApiConfig config;
  @Autowired
  private EventService eventService;
  @Autowired
  private SharedService sharedService;

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
   * Validates a generated report
   *
   * @param tenantId The id of the tenant
   * @param reportId The id of the report to validate against
   * @param severity The minimum severity level to report on
   * @return Returns an OperationOutcome resource that provides details about each of the issues found
   * @throws IOException
   */
  @GetMapping("/{tenantId}/{reportId}")
  public OperationOutcome getValidation(@PathVariable String tenantId, @PathVariable String reportId, @RequestParam(defaultValue = "INFORMATION") OperationOutcome.IssueSeverity severity) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);

    if (tenantService == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    Report report = tenantService.getReport(reportId);

    if (report == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found");
    }

    Bundle submissionBundle = Helper.generateBundle(tenantService, report, this.eventService, this.config);
    StopwatchManager stopwatchManager = new StopwatchManager(this.sharedService);

    //Only determine metrics for validation explicitly done on a stored report (through request path vars)
    OperationOutcome outcome;
    try(Stopwatch stopwatch = stopwatchManager.start(Constants.TASK_VALIDATE, Constants.CATEGORY_VALIDATION)) {
      outcome = this.validator.validate(submissionBundle, severity);
    }
    stopwatchManager.storeMetrics(tenantId, reportId);

    if (report.getDeviceInfo() != null) {
      outcome.addContained(report.getDeviceInfo());
    }

    return outcome;
  }

  /**
   * Provides a summary of unique messages from validation results
   *
   * @param tenantId The id of the tenant
   * @param reportId The id of the report to validate against
   * @param severity The minimum severity level to report on
   * @return Returns a plain string, each line representing a single message (including severity)
   * @throws IOException
   */
  @GetMapping("/{tenantId}/{reportId}/summary")
  public String getValidationSummary(@PathVariable String tenantId, @PathVariable String reportId, @RequestParam(defaultValue = "INFORMATION") OperationOutcome.IssueSeverity severity) throws IOException {
    OperationOutcome outcome = this.validate(tenantId, reportId, severity);
    return this.getValidationSummary(outcome);
  }

  @PostMapping("/{tenantId}/{reportId}/$validate")
  public OperationOutcome validate(@PathVariable String tenantId, @PathVariable String reportId, @RequestParam(defaultValue = "INFORMATION") OperationOutcome.IssueSeverity severity) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);

    if (tenantService == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    Report report = tenantService.getReport(reportId);

    if (report == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found");
    }

    Bundle bundle = Helper.generateBundle(tenantService, report, this.eventService, this.config);
    OperationOutcome outcome = this.validator.validate(bundle, severity);

    tenantService.deleteValidationResults(reportId);
    tenantService.insertValidationResults(reportId, outcome.getIssue());

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
}
