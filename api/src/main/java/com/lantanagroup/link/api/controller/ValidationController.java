package com.lantanagroup.link.api.controller;

import com.lantanagroup.link.EventService;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.Helper;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.Report;
import com.lantanagroup.link.validation.Validator;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/{tenantId}/validate")
public class ValidationController {

  private static final Logger logger = LoggerFactory.getLogger(ValidationController.class);
  @Autowired
  private SharedService sharedService;
  @Autowired
  private ApiConfig config;
  @Autowired
  private EventService eventService;

  private Bundle validateBundle(TenantService tenantService, Bundle bundle, OperationOutcome.IssueSeverity severity) {
    try {
      Bundle result = new Bundle()
              .setType(Bundle.BundleType.COLLECTION);

      Device device = FhirHelper.getDevice(config);
      result.addEntry().setResource(device);

      Validator validator = new Validator(tenantService.getConfig().getValidation());
      OperationOutcome outcome = validator.validate(bundle, severity);
      result.addEntry().setResource(outcome);

      Path tempFile = Files.createTempFile(null, ".json");
      try (FileWriter fw = new FileWriter(tempFile.toFile())) {
        FhirContextProvider.getFhirContext().newJsonParser().encodeResourceToWriter(outcome, fw);
      }
      logger.info("Validation results saved to {}", tempFile);

      // Add an extension (which doesn't formally exist) that shows the total issues
      outcome.addExtension("http://nhsnlink.org/oo-total", new IntegerType(outcome.getIssue().size()));

      // Don't return more than 2k issues to the response... We can just look at the temp file instead
      if (outcome.getIssue().size() > 2000) {
        outcome.setIssue(null);
      } else {
        outcome.getIssue().forEach(i -> {
          i.setExtension(null);
          i.setDetails(null);
          if (i.getLocation().size() == 2) {
            i.getLocation().remove(1);    // Remove the line number - it's useless.
          }
        });
      }

      return result;
    } catch (IOException ex) {
      logger.error("Error storing bundle validation results to file", ex);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private OperationOutcome getOperationOutcome(Bundle bundle) {
    return bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> resource instanceof OperationOutcome)
            .map(resource -> (OperationOutcome) resource)
            .findFirst()
            .orElseThrow();
  }

  /**
   * Validates a Bundle provided in the request body
   *
   * @param tenantId The id of the tenant
   * @param severity The minimum severity level to report on
   * @return Returns an OperationOutcome resource that provides details about each of the issues found
   */
  @PostMapping
  public Bundle validate(@PathVariable String tenantId, @RequestBody Bundle bundle, @RequestParam(defaultValue = "WARNING") OperationOutcome.IssueSeverity severity) {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);

    if (tenantService == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    return this.validateBundle(tenantService, bundle, severity);
  }

  private String getValidationSummary(OperationOutcome outcome) {
    List<String> uniqueMessages = new ArrayList<>();

    for (OperationOutcome.OperationOutcomeIssueComponent issue : outcome.getIssue()) {
      String message = issue.getSeverity().toString() + ": " + issue.getDiagnostics();
      if (!uniqueMessages.contains(message)) {
        uniqueMessages.add(message);
      }
    }

    if (!uniqueMessages.isEmpty()) {
      return "* " + String.join("\n* ", uniqueMessages);
    }

    return "No issues found";
  }

  /**
   * Validates a Bundle provided in the request body
   *
   * @param tenantId The id of the tenant
   * @param severity The minimum severity level to report on
   * @return Returns an OperationOutcome resource that provides details about each of the issues found
   */
  @PostMapping("/summary")
  public String validateSummary(@PathVariable String tenantId, @RequestBody Bundle bundle, @RequestParam(defaultValue = "WARNING") OperationOutcome.IssueSeverity severity) {
    Bundle result = this.validate(tenantId, bundle, severity);
    OperationOutcome outcome = this.getOperationOutcome(result);
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
  @GetMapping("/{reportId}")
  public Bundle validate(@PathVariable String tenantId, @PathVariable String reportId, @RequestParam(defaultValue = "WARNING") OperationOutcome.IssueSeverity severity) throws IOException {
    TenantService tenantService = TenantService.create(this.sharedService, tenantId);

    if (tenantService == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
    }

    Report report = tenantService.getReport(reportId);

    if (report == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found");
    }

    Bundle submissionBundle = Helper.generateBundle(tenantService, report, this.eventService, this.config);
    return this.validateBundle(tenantService, submissionBundle, severity);
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
  @GetMapping("/{reportId}/summary")
  public String validateSummary(@PathVariable String tenantId, @PathVariable String reportId, @RequestParam(defaultValue = "WARNING") OperationOutcome.IssueSeverity severity) throws IOException {
    Bundle result = this.validate(tenantId, reportId, severity);
    OperationOutcome outcome = this.getOperationOutcome(result);
    return this.getValidationSummary(outcome);
  }
}
