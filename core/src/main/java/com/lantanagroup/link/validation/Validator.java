package com.lantanagroup.link.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import com.lantanagroup.link.FhirContextProvider;
import com.lantanagroup.link.db.model.tenant.Validation;
import org.hl7.fhir.common.hapi.validation.support.*;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r5.utils.validation.constants.BestPracticeWarningLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Validator {
  protected static final Logger logger = LoggerFactory.getLogger(Validator.class);

  private FhirValidator validator;

  public Validator(Validation config) {
    try {
      FhirContext ctx = FhirContextProvider.getFhirContext();
      NpmPackageValidationSupport npmPackageSupport = new NpmPackageValidationSupport(ctx);
      for (String npmPackage : config.getNpmPackages()) {
        logger.info("Validating against NPM package: {}", npmPackage);
        npmPackageSupport.loadPackageFromClasspath(npmPackage);
      }
      ValidationSupportChain validationSupportChain = new ValidationSupportChain(
              new DefaultProfileValidationSupport(ctx),
              new CommonCodeSystemsTerminologyService(ctx),
              npmPackageSupport,
              new InMemoryTerminologyServerValidationSupport(ctx)
      );
      CachingValidationSupport validationSupport = new CachingValidationSupport(validationSupportChain);
      this.validator = ctx.newValidator();

      FhirInstanceValidator instanceValidator = new FhirInstanceValidator(validationSupport);
      instanceValidator.setValidatorPolicyAdvisor(new PolicyAdvisor());
      instanceValidator.setAssumeValidRestReferences(true);
      instanceValidator.setErrorForUnknownProfiles(false);
      instanceValidator.setBestPracticeWarningLevel(BestPracticeWarningLevel.Error);
      this.validator.registerValidatorModule(instanceValidator);
      this.validator.setConcurrentBundleValidation(true);
    } catch (IOException ex) {
      logger.error("Error initializing validator", ex);
    }
  }

  public OperationOutcome validate(Resource resource, OperationOutcome.IssueSeverity severity) {
    logger.debug("Validating {}", resource.getResourceType().toString().toLowerCase());
    Date start = new Date();
    ValidationResult result = this.validator.validateWithResult(resource);
    Date end = new Date();
    logger.debug("Validation took {} seconds", TimeUnit.MILLISECONDS.toSeconds(end.getTime() - start.getTime()));
    OperationOutcome outcome = (OperationOutcome) result.toOperationOutcome();
    outcome.setIssue(outcome.getIssue().stream()
            .filter(i -> {
              boolean isError = i.getSeverity() == OperationOutcome.IssueSeverity.ERROR;
              boolean isWarning = i.getSeverity() == OperationOutcome.IssueSeverity.WARNING;
              boolean isInfo = i.getSeverity() == OperationOutcome.IssueSeverity.INFORMATION;

              if (severity == OperationOutcome.IssueSeverity.ERROR && !isError) {
                return false;
              } else if (severity == OperationOutcome.IssueSeverity.WARNING && !isError && !isWarning) {
                return false;
              } else if (severity == OperationOutcome.IssueSeverity.INFORMATION && !isError && !isWarning && !isInfo) {
                return false;
              }
              return true;
            })
            .collect(Collectors.toList()));
    logger.debug("Done validating and found {} errors", outcome.getIssue().size());

    return outcome;
  }
}
