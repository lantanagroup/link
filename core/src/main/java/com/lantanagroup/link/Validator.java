package com.lantanagroup.link;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import org.hl7.fhir.common.hapi.validation.support.*;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
public class Validator {
  protected static final Logger logger = LoggerFactory.getLogger(Validator.class);

  private FhirValidator validator;

  public Validator() {
    try {
      FhirContext ctx = FhirContextProvider.getFhirContext();
      NpmPackageValidationSupport npmPackageSupport = new NpmPackageValidationSupport(ctx);
      npmPackageSupport.loadPackageFromClasspath("classpath:nhsn-measures.tgz");
      npmPackageSupport.loadPackageFromClasspath("classpath:uscore.tgz");
      npmPackageSupport.loadPackageFromClasspath("classpath:qicore.tgz");
      npmPackageSupport.loadPackageFromClasspath("classpath:deqm.tgz");
      ValidationSupportChain validationSupportChain = new ValidationSupportChain(
              npmPackageSupport,
              new DefaultProfileValidationSupport(ctx),
              new CommonCodeSystemsTerminologyService(ctx),
              new InMemoryTerminologyServerValidationSupport(ctx)
      );
      CachingValidationSupport validationSupport = new CachingValidationSupport(validationSupportChain);
      this.validator = ctx.newValidator();

      FhirInstanceValidator instanceValidator = new FhirInstanceValidator(validationSupport);
      this.validator.registerValidatorModule(instanceValidator);
      this.validator.setConcurrentBundleValidation(true);
    } catch (IOException ex) {
      logger.error("Error initializing validator", ex);
    }
  }

  public OperationOutcome validate(Resource resource, OperationOutcome.IssueSeverity severity) {
    logger.debug("Validating {}", resource.getResourceType().toString().toLowerCase());
    ValidationResult result = this.validator.validateWithResult(resource);
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
