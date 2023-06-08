package com.lantanagroup.link.validation;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.db.model.tenant.Validation;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Collectors;

public class ValidatorTests {
  protected static final Logger logger = LoggerFactory.getLogger(ValidatorTests.class);
  private FhirContext ctx = FhirContext.forR4();
  private Validator validator;

  private Validator getValidator() {
    if (this.validator == null) {
      Validation validation = new Validation();
      validation.getNpmPackages().add("nhsn-measures.tgz");
      this.validator = new Validator(validation);

      // Perform a single validation to pre-load all the packages and profiles
      this.validator.validate(new Bundle(), OperationOutcome.IssueSeverity.ERROR);
      logger.info("Done initializing validator");
    }

    return this.validator;
  }

  private Bundle getBundle() throws IOException {
    return this.ctx.newJsonParser().parseResource(
            Bundle.class,
            Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("large-submission-example.json")));
  }

  @Test
  @Ignore
  public void testPerformance() throws IOException {
    Bundle bundle = this.getBundle();
    OperationOutcome oo = this.getValidator().validate(bundle, OperationOutcome.IssueSeverity.INFORMATION);

    logger.info("Issues:");
    oo.getIssue().forEach(i -> {
      logger.info("{} - {} - {}", i.getSeverity(), i.getLocation(), i.getDiagnostics());
    });
  }

  @Test
  public void testValidationCredibility() throws IOException {
    var bundle = this.getBundle();
    var patientResources = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof Patient)
            .map(r -> (Patient) r)
            .collect(Collectors.toList());

    ValidateBundle(bundle, true);

    for (Patient patient : patientResources)
    {
      patient.getName().clear();
    }

    ValidateBundle(bundle, false);
  }

  private void ValidateBundle(Bundle bundle, Boolean failOnIssueFound)
  {
    OperationOutcome oo = this.getValidator().validate(bundle, OperationOutcome.IssueSeverity.ERROR);

    logger.info("Beginning Validation. Fail State: {}", failOnIssueFound ? "Issues Found" : "No Issues Found");
    if(oo.hasIssue()) {
      logger.info("Issues Found:");
      oo.getIssue().forEach(i -> logger.info(i.getDiagnostics()));
    }
    else {
      logger.info("No Issues Found.");
    }

    //If we want to fail the test if the bundle has errors
    if(failOnIssueFound)
      Assert.assertFalse(oo.hasIssue());
    //If we want to fail the test if the bundle does not have errors
    else
      Assert.assertTrue(oo.hasIssue());
  }
}
