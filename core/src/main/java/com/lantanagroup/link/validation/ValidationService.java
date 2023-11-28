package com.lantanagroup.link.validation;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.EventService;
import com.lantanagroup.link.Helper;
import com.lantanagroup.link.ValidationCategorizer;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.TenantService;
import com.lantanagroup.link.db.model.Report;
import com.lantanagroup.link.db.model.tenant.ValidationResult;
import com.lantanagroup.link.db.model.tenant.ValidationResultCategory;
import com.lantanagroup.link.time.Stopwatch;
import com.lantanagroup.link.time.StopwatchManager;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ValidationService {
  @Autowired
  private SharedService sharedService;

  @Autowired
  private Validator validator;

  @Autowired
  private EventService eventService;

  @Autowired
  private ApiConfig config;

  public OperationOutcome validate(StopwatchManager stopwatchManager, TenantService tenantService, Report report) {
    OperationOutcome outcome;

    Bundle bundle = Helper.generateBundle(tenantService, report, this.eventService, this.config);

    try (Stopwatch stopwatch = stopwatchManager.start(Constants.TASK_VALIDATE, Constants.CATEGORY_VALIDATION)) {
      // Always get information severity level so that we persist all possible issues, but only return the severity asked
      // for when making requests to the REST API.
      outcome = this.validator.validate(bundle, OperationOutcome.IssueSeverity.INFORMATION);

      tenantService.deleteValidationResults(report.getId());
      tenantService.insertValidationResults(report.getId(), outcome);

      // Re-get the results that now have a result id associated to each entry
      List<ValidationResult> results = tenantService.getValidationResults(report.getId(), OperationOutcome.IssueSeverity.INFORMATION);
      ValidationCategorizer categorizer = new ValidationCategorizer(results);
      categorizer.loadFromResources();
      List<ValidationResultCategory> categorizedResults = categorizer.categorize();

      tenantService.deleteValidationCategoriesForReport(report.getId());
      tenantService.insertValidationResultCategories(categorizedResults);
    }

    return outcome;
  }
}
