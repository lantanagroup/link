package com.lantanagroup.link.validation;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.db.model.tenant.Validation;
import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
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
      validation.getNpmPackages().add("cqfmeasures.tgz");
      validation.getNpmPackages().add("qicore.tgz");
      validation.getNpmPackages().add("uscore.tgz");
      validation.getNpmPackages().add("deqm.tgz");
      validation.getNpmPackages().add("terminology.tgz");
      this.validator = new Validator(validation);

      // Perform a single validation to pre-load all the packages and profiles
      this.validator.validate(new Bundle(), OperationOutcome.IssueSeverity.ERROR);
      logger.info("Done initializing validator");
    }

    return this.validator;
  }

  private Bundle getBundle(String resourcePath) {
    return this.ctx.newJsonParser().parseResource(
            Bundle.class,
            Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream(resourcePath)));
  }

  @Test
  @Ignore
  public void testPerformance() throws IOException {
    Bundle bundle = this.getBundle("large-submission-example.json");
    OperationOutcome oo = this.getValidator().validate(bundle, OperationOutcome.IssueSeverity.INFORMATION);

    logger.info("Issues:");
    oo.getIssue().forEach(i -> {
      logger.info("{} - {} - {}", i.getSeverity(), i.getLocation(), i.getDiagnostics());
    });
  }

  @Test
  public void validateUsCore() throws IOException {
    var bundle = this.getBundle("large-submission-example.json");
    var patientResource = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof Patient)
            .map(r -> (Patient) r)
            .findFirst();

    validateBundle(bundle, true);

    patientResource.get().getName().clear();

    validateBundle(bundle, false);
  }

  @Test
  public void validateNhsnMeasureIg() throws IOException {
    var bundle = this.getBundle("large-submission-example.json");

    var organizationResource = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof Organization)
            .map(r -> (Organization) r)
            .collect(Collectors.toList());

    var encounter = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof Encounter)
            .map(r -> (Encounter) r)
            .findFirst();

    validateBundle(bundle, true);

    organizationResource.forEach(o ->bundle.getEntry().remove(o));

    encounter.get().getClass_().setCode("UNKNOWNCODE");

    validateBundle(bundle, false);
  }

  @Test
  public void validateDqmIg() throws IOException {
    var bundle = this.getBundle("large-submission-example.json");

    var measureReport = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof MeasureReport)
            .map(r -> (MeasureReport) r)
            .findFirst();

    validateBundle(bundle, true);

    measureReport.get().setMeasure("");

    validateBundle(bundle, false);
  }

  private void validateBundle(Bundle bundle, Boolean failOnIssueFound) {
    OperationOutcome oo = this.getValidator().validate(bundle, OperationOutcome.IssueSeverity.ERROR);

    List<String> allMessages = oo.getIssue().stream().map(i -> {
      return i.getDiagnostics()
              .replaceAll("\\sPatient\\/.+?\\s", " Patient/X ")
              .replaceAll("\\sMeasureReport\\/.+?\\s", " MeasureReport/X ");
    }).collect(Collectors.toList());
    List<String> distinctMessages = allMessages.stream().distinct().collect(Collectors.toList());

    logger.info("Beginning Validation. Fail State: {}", failOnIssueFound ? "Issues Found" : "No Issues Found");

    //If we want to fail the test if the bundle has errors
    if (failOnIssueFound) {
      Assert.assertFalse(oo.hasIssue());
      //If we want to fail the test if the bundle does not have errors
    } else {
      Assert.assertTrue(oo.hasIssue());
    }
  }
}
