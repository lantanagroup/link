package com.lantanagroup.link.validation;

import com.lantanagroup.link.Constants;
import com.lantanagroup.link.TestHelper;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.db.SharedService;
import com.lantanagroup.link.db.model.MeasureDefinition;
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
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Ignore
public class ValidatorTests {
  protected static final Logger logger = LoggerFactory.getLogger(ValidatorTests.class);
  private static Validator validator;
  private static Bundle largeBundle;

  /**
   * The validator should be initialized outside the actual tests so that we can determine the difference between the
   * time to initialize the validator and the time it takes to validate a single bundle post-initialization
   */
  @BeforeClass
  public static void init() {
    largeBundle = TestHelper.getBundle("large-submission-example.json");

    MeasureDefinition measureDefinition = new MeasureDefinition();
    measureDefinition.setBundle(new Bundle());
    measureDefinition.getBundle().addEntry()
            .setResource(new Measure().setUrl("http://test.com/fhir/Measure/a"));
    SharedService sharedService = mock(SharedService.class);
    when(sharedService.getMeasureDefinitions()).thenReturn(List.of(measureDefinition));

    if (validator == null) {
      validator = new Validator(sharedService, new ApiConfig());
      validator.init();

      // Perform a single validation to pre-load all the packages and profiles
      validator.validate(new Bundle(), OperationOutcome.IssueSeverity.ERROR);
      logger.info("Done initializing validator");
    }
  }

  @Test
  public void testPerformanceWithInit() {
    logger.info("Testing initialization of validator");

    MeasureDefinition measureDefinition = new MeasureDefinition();
    measureDefinition.setBundle(new Bundle());
    measureDefinition.getBundle().addEntry()
            .setResource(new Measure().setUrl("http://test.com/fhir/Measure/a"));
    SharedService sharedService = mock(SharedService.class);
    when(sharedService.getMeasureDefinitions()).thenReturn(List.of(measureDefinition));

    Validator newValidator = new Validator(sharedService, new ApiConfig());
    newValidator.init();

    // Perform a single validation to pre-load all the packages and profiles
    newValidator.validate(largeBundle, OperationOutcome.IssueSeverity.ERROR);
    logger.info("Done testing initialization of validator");
  }

  @Test
  public void testPerformanceValidating() throws IOException {
    OperationOutcome oo = validator.validate(largeBundle, OperationOutcome.IssueSeverity.INFORMATION);

    logger.info("Issues: {}", oo.getIssue().size());
    /*oo.getIssue().forEach(i -> {
      logger.info("{} - {} - {}", i.getSeverity(), i.getLocation(), i.getDiagnostics());
    });*/
  }

  @Test
  public void validateUsCore() throws IOException {
    Patient patient = new Patient();
    patient.getMeta().addProfile("http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient");
    patient.addIdentifier()
            .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
            .setValue("103");
    patient.addName()
            .addGiven("Joe")
            .setFamily("Somebody");
    patient.setGender(Enumerations.AdministrativeGender.MALE);

    OperationOutcome oo = validator.validate(patient, OperationOutcome.IssueSeverity.ERROR);
    Assert.assertEquals(0, oo.getIssue().size());

    patient.setGender(null);
    oo = validator.validate(patient, OperationOutcome.IssueSeverity.ERROR);
    Assert.assertEquals(1, oo.getIssue().size());
    Assert.assertEquals("Patient.gender: minimum required = 1, but only found 0 (from http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient)", oo.getIssue().get(0).getDiagnostics());
  }

  @Test
  public void validateNhsnMeasureIg() throws IOException {
    var bundle1 = TestHelper.getBundle("single-submission-example.json");

    OperationOutcome oo = validator.validate(bundle1, OperationOutcome.IssueSeverity.ERROR);
    Assert.assertEquals(0, oo.getIssue().size());

    // Remove the organization from the bundle which is required by the top level bundle profile
    Bundle bundle2 = bundle1.copy();
    bundle2.getEntry().removeIf(e -> e.getResource().getResourceType() == ResourceType.Organization);
    oo = validator.validate(bundle2, OperationOutcome.IssueSeverity.ERROR);
    Assert.assertEquals(2, oo.getIssue().size());
    Assert.assertEquals("Rule bundle-contain-all-measurereport-references: 'MeasureReport Bundle: must contain all Resources that are references by MeasureReport references' Failed", oo.getIssue().get(0).getDiagnostics());
    Assert.assertEquals("Bundle.entry:submitting-organization: minimum required = 1, but only found 0 (from http://www.cdc.gov/nhsn/fhirportal/dqm/ig/StructureDefinition/nhsn-measurereport-bundle)", oo.getIssue().get(1).getDiagnostics());

    // Remove MeasureReport.group.population.count which is required by subject-list profiles
    Bundle bundle3 = bundle1.copy();
    bundle3.getEntry().stream()
            .filter(e -> e.getResource().getResourceType() == ResourceType.MeasureReport)
            .map(e -> (MeasureReport) e.getResource())
            .forEach(mr -> mr.getGroup().forEach(g -> g.getPopulation().forEach(p -> p.setCountElement(null))));

    oo = validator.validate(bundle3, OperationOutcome.IssueSeverity.ERROR);
    Assert.assertEquals(3, oo.getIssue().size());
    Assert.assertEquals("MeasureReport.group.population.count: minimum required = 1, but only found 0 (from http://www.cdc.gov/nhsn/fhirportal/dqm/ig/StructureDefinition/subjectlist-measurereport)", oo.getIssue().get(0).getDiagnostics());
    Assert.assertEquals("MeasureReport.group.population.count: minimum required = 1, but only found 0 (from http://www.cdc.gov/nhsn/fhirportal/dqm/ig/StructureDefinition/subjectlist-measurereport)", oo.getIssue().get(1).getDiagnostics());
    Assert.assertEquals("Bundle.entry:subject-list: minimum required = 1, but only found 0 (from http://www.cdc.gov/nhsn/fhirportal/dqm/ig/StructureDefinition/nhsn-measurereport-bundle)", oo.getIssue().get(2).getDiagnostics());
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
