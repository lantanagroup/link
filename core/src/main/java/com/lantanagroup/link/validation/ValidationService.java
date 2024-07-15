package com.lantanagroup.link.validation;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.EventService;
import com.lantanagroup.link.FhirBundler;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.mappers.ValidationResultMapper;
import com.lantanagroup.link.db.model.MeasureDefinition;
import com.lantanagroup.link.db.model.MetricData;
import com.lantanagroup.link.db.model.Metrics;
import com.lantanagroup.link.db.model.Report;
import com.lantanagroup.link.db.model.tenant.ValidationResult;
import com.lantanagroup.link.db.model.tenant.ValidationResultCategory;
import com.lantanagroup.link.time.Stopwatch;
import com.lantanagroup.link.time.StopwatchManager;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ValidationService {
  private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);

  @Autowired
  private SharedService sharedService;

  @Autowired
  private Validator validator;

  @Autowired
  private EventService eventService;

  public OperationOutcome validate(StopwatchManager stopwatchManager, TenantService tenantService, Report report) {
    List<Bundle> measureDefinitions = report.getMeasureIds().stream()
            .map(sharedService::getMeasureDefinition)
            .filter(Objects::nonNull)
            .map(MeasureDefinition::getBundle)
            .collect(Collectors.toList());

    OperationOutcome outcome;

    FhirBundler bundler = new FhirBundler(this.eventService, this.sharedService, tenantService);
    Bundle bundle = bundler.generateBundle(report);

    ValidationCategorizer categorizer = new ValidationCategorizer();
    categorizer.loadFromResources();

    try (Stopwatch stopwatch = stopwatchManager.start(Constants.TASK_VALIDATE, Constants.CATEGORY_VALIDATION)) {
      // Always get information severity level so that we persist all possible issues, but only return the severity asked
      // for when making requests to the REST API.
      outcome = this.validator.validate(bundle, OperationOutcome.IssueSeverity.INFORMATION, measureDefinitions);

      tenantService.deleteValidationResults(report.getId());

      // In batches of 100 ...
      int batchSize = 100;
      for (int batchIndex = 0; ; batchIndex++) {
        long startIndex = (long) batchIndex * batchSize;
        if (startIndex >= outcome.getIssue().size()) {
          break;
        }
        logger.debug("Categorizing and saving validation results: {}/{}", startIndex, outcome.getIssue().size());

        // Map OO issues to persistable results
        List<ValidationResult> results = outcome.getIssue().stream()
                .skip(startIndex)
                .limit(batchSize)
                .map(ValidationResultMapper::toValidationResult)
                .peek(model -> model.setId(UUID.randomUUID()))
                .collect(Collectors.toList());

        // Categorize and save results
        List<ValidationResultCategory> categorizedResults = categorizer.categorize(results);
        tenantService.insertValidationResults(report.getId(), results);
        tenantService.insertValidationResultCategories(categorizedResults);
      }
    }

    //log validation issue metrics
    List<Metrics> metrics = new ArrayList<>();
    Metrics metric = new Metrics();
    metrics.add(metric);
    String task = Constants.VALIDATION_ISSUE_TASK;
    String category = Constants.VALIDATION_ISSUE_CATEGORY;
    metric.setTenantId(tenantService.getConfig().getId());
    metric.setReportId(report.getId());
    metric.setVersion(report.getVersion());
    metric.setTaskName(task);
    metric.setCategory(category);
    metric.setTimestamp(new Date());

    MetricData data = new MetricData();
    data.count = outcome.getIssue().size();
    metric.setData(data);

    this.sharedService.saveMetrics(metrics);

    return outcome;
  }
}
