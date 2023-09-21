package com.lantanagroup.link.validation;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.db.model.tenant.Validation;
import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Ignore
public class ValidatorTests {
  protected static final Logger logger = LoggerFactory.getLogger(ValidatorTests.class);
  private static FhirContext ctx = FhirContext.forR4();
  private static Validator validator;

  /**
   * The validator should be initialized outside the actual tests so that we can determine the difference between the
   * time to initialize the validator and the time it takes to validate a single bundle post-initialization
   */
  @BeforeClass
  public static void init() {
    if (validator == null) {
      Validation validation = new Validation();
      validation.getNpmPackages().add("nhsn-measures.tgz");
      validation.getNpmPackages().add("cqfmeasures.tgz");
      validation.getNpmPackages().add("qicore.tgz");
      validation.getNpmPackages().add("uscore.tgz");
      validation.getNpmPackages().add("deqm.tgz");
      validation.getNpmPackages().add("terminology.tgz");
      validator = new Validator(validation);

      // Perform a single validation to pre-load all the packages and profiles
      validator.validate(new Bundle(), OperationOutcome.IssueSeverity.ERROR);
      logger.info("Done initializing validator");
    }
  }

  private Bundle getBundle(String resourcePath) {
    return ctx.newJsonParser().parseResource(
            Bundle.class,
            Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream(resourcePath)));
  }

  @Test
  public void testPerformance() throws IOException {
    Bundle bundle = this.getBundle("large-submission-example.json");
    OperationOutcome oo = validator.validate(bundle, OperationOutcome.IssueSeverity.INFORMATION);

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

    validate(bundle, true);

    patientResource.get().getName().clear();

    validate(bundle, false);
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

    validate(bundle, true);

    organizationResource.forEach(o ->bundle.getEntry().remove(o));

    encounter.get().getClass_().setCode("UNKNOWNCODE");

    validate(bundle, false);
  }

  @Test
  public void validateDeqmProfiles() {
    MeasureReport report = new MeasureReport();
    report.setMeta(new Meta().addProfile(Constants.IndividualMeasureReportProfileUrl));
    report.setMeasure("http://test.com");
    report.setType(MeasureReport.MeasureReportType.INDIVIDUAL);
    report.setStatus(MeasureReport.MeasureReportStatus.COMPLETE);
    report.setPeriod(new Period().setStart(new Date(2023, 1, 1)).setEnd(new Date(2023, 1, 31)));
    report.setDate(new Date());
    report.setReporter(new Reference("Organization/123"));
    report.setSubject(new Reference("Patient/321"));
    report.addExtension(Constants.MeasureScoringExtension, new CodeableConcept(new Coding().setSystem(Constants.MeasureScoringCodeSystem).setCode("proportion")));
    report.setImprovementNotation(new CodeableConcept(new Coding().setSystem(Constants.MeasureImprovementNotationCodeSystem).setCode("increase")));

    // Expect valid
    OperationOutcome outcome = validator.validate(report, OperationOutcome.IssueSeverity.ERROR);
    Assert.assertTrue(outcome.getIssue().isEmpty());

    // Break the report, should be invalid, 2 errors from DEQM IG, one from core FHIR
    report.setMeasure(null);
    report.setImprovementNotation(null);
    outcome = validator.validate(report, OperationOutcome.IssueSeverity.ERROR);
    Assert.assertEquals(3, outcome.getIssue().size());
    Assert.assertEquals("MeasureReport.measure: minimum required = 1, but only found 0 (from http://hl7.org/fhir/StructureDefinition/MeasureReport)", outcome.getIssue().get(0).getDiagnostics());
    Assert.assertEquals("Rule deqm-2: 'If the measure scoring type is 'proportion','ratio', or 'continuous-variable' then the improvementNotation element is required' Failed", outcome.getIssue().get(1).getDiagnostics());
    Assert.assertEquals("MeasureReport.measure: minimum required = 1, but only found 0 (from http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/indv-measurereport-deqm)", outcome.getIssue().get(2).getDiagnostics());
  }

  private void validate(Resource resource, Boolean failOnIssueFound) {
    OperationOutcome oo = validator.validate(resource, OperationOutcome.IssueSeverity.ERROR);

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
