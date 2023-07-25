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
  @Ignore
  public void validateUsCore_Patient_Name_Remove() throws IOException {

    var bundle = this.getBundle("large-submission-example.json");

    var patientResource = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof Patient)
            .map(r -> (Patient) r)
            .findFirst();

    var issueTextToFind = "Patient.name: minimum required = 1, but only found 0";

    ValidateBundle(bundle, issueTextToFind, true);

    patientResource.get().getName().clear();

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  @Ignore
  public void validateUsCore_Patient_Identifier_Remove() throws IOException {

    var bundle = this.getBundle("large-submission-example.json");

    var patientResource = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof Patient)
            .map(r -> (Patient) r)
            .findFirst();

    var issueTextToFind = "Patient.identifier: minimum required = 1, but only found 0";

    ValidateBundle(bundle, issueTextToFind, true);

    patientResource.get().getIdentifier().clear();

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  @Ignore
  public void validateUsCore_Patient_Gender_Remove() throws IOException {

    var bundle = this.getBundle("large-submission-example.json");

    var patientResource = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof Patient)
            .map(r -> (Patient) r)
            .findFirst();

    var issueTextToFind = "The value provided ('?') is not in the value set 'AdministrativeGender'";

    ValidateBundle(bundle, issueTextToFind, true);

    patientResource.get().setGender(Enumerations.AdministrativeGender.NULL);

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  @Ignore
  public void validateUsCore_Encounter_Class_Remove() throws IOException {

    var bundle = this.getBundle("new-sira-bundle.json");

    var encounter = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof Encounter)
            .map(r -> (Encounter) r)
            .findFirst();

    var issueTextToFind = "Encounter.class: minimum required = 1, but only found 0";

    ValidateBundle(bundle, issueTextToFind, true);

    encounter.get().setClass_(null);

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  @Ignore
  public void validateUsCore_Encounter_Status_Remove() throws IOException {

    var bundle = this.getBundle("new-sira-bundle.json");

    var encounter = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof Encounter)
            .map(r -> (Encounter) r)
            .findFirst();

    var issueTextToFind = "Encounter.status: minimum required = 1, but only found 0";

    ValidateBundle(bundle, issueTextToFind, true);

    encounter.get().setStatus(null);

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  @Ignore
  public void validateUsCore_Encounter_Type_Remove() throws IOException {

    var bundle = this.getBundle("new-sira-bundle.json");

    var encounters = bundle.getEntry().stream()
            .filter(r -> r.getResource() instanceof Encounter)
            .collect(Collectors.toList());

    for(var encounter : encounters)
    {
      if(!((Encounter)encounter.getResource()).hasType())
        bundle.getEntry().remove(encounter);
    }

    var issueTextToFind = "Encounter.type: minimum required = 1, but only found 0 ";

    ValidateBundle(bundle, issueTextToFind, true);

    for(var encounter : encounters)
    {
      ((Encounter)encounter.getResource()).setType(null);
    }

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  @Ignore
  public void validateUsCore_Encounter_Class_Change() throws IOException {

    var bundle = this.getBundle("new-sira-bundle.json");

    var encounter = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof Encounter)
            .map(r -> (Encounter) r)
            .findFirst();

    var issueTextToFind = "Unknown Code";

    ValidateBundle(bundle, issueTextToFind, true);

    encounter.get().getClass_().setCode("Bogus Code To Fail");

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  @Ignore
  public void validateNhsnMeasureIg_Organization_Remove() throws IOException {

    var bundle = this.getBundle("large-submission-example2.json");

    var organizationResource = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof Organization)
            .map(r -> (Organization)r)
            .findFirst();

    var issueTextToFind = "Bundle.entry:organization: minimum required = 1, but only found 0";

    ValidateBundle(bundle, issueTextToFind, true);

    organizationResource.get().setName("");
    organizationResource.get().setId("");

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  public void validateNhsnMeasureIg_Location_Remove() throws IOException {

    var bundle = this.getBundle("new-sira-bundle.json");

    var encounter = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof Encounter)
            .map(r -> (Encounter) r)
            .findFirst();

    var issueTextToFind = "Encounter.location.location: minimum required = 1, but only found 0";

    ValidateBundle(bundle, issueTextToFind, true);

    if(!encounter.get().getLocation().isEmpty()) {
      for(var encLoc : encounter.get().getLocation()){
        encLoc.setLocation(null);
      }
    }

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  public void validateNhsnMeasureIg_LocationStatus_NULL() throws IOException {

    var bundle = this.getBundle("new-sira-bundle.json");

    var encounter = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof Encounter)
            .map(r -> (Encounter) r)
            .findFirst();

    var issueTextToFind = "The value provided ('?') is not in the value set 'EncounterLocationStatus'";

    ValidateBundle(bundle, issueTextToFind, true);

    if(!encounter.get().getLocation().isEmpty()) {
      for(var encLoc : encounter.get().getLocation()){
        encLoc.setStatus(Encounter.EncounterLocationStatus.NULL);
      }
    }

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  @Ignore
  public void validateNhsnMeasureIg_MedicationAdministration_Remove() throws IOException {

    var bundle = this.getBundle("new-sira-bundle.json");

    var observations = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof Observation)
            .map(r -> (Observation) r)
            .collect(Collectors.toList());

    var issueTextToFind = "Observation.code: minimum required = 1, but only found 0";

    ValidateBundle(bundle, issueTextToFind, true);

    for(var obs: observations)
    {
      obs.setId("");
      obs.setCode(null);
      obs.setEncounter(null);
    }

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  @Ignore
  public void validateNhsnMeasureIg_BodyStructure_Change() throws IOException {

    var bundle = this.getBundle("large-submission-example2.json");

    var bodyStructureResources = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof BodyStructure)
            .map(r -> (BodyStructure) r)
            .collect(Collectors.toList());

    var issueTextToFind = "body";

    if(bodyStructureResources.size() > 0) {
      ValidateBundle(bundle, issueTextToFind, true);

      for (var bod : bodyStructureResources) {
        bod.setId("");
        bod.setPatient(null);
        bod.setLocation(null);
      }

      ValidateBundle(bundle, issueTextToFind, false);
    }
    else
    {
      Assert.assertTrue(true);
    }
  }

  @Test
  @Ignore
  public void validateDqmIg_MeasureReport_Measure() throws IOException {

    var bundle = this.getBundle("large-submission-example.json");

    var measureReport = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof MeasureReport)
            .map(r -> (MeasureReport) r)
            .findFirst();

    var issueTextToFind = "MeasureReport.measure: minimum required = 1, but only found 0";

    ValidateBundle(bundle, issueTextToFind, true);

    measureReport.get().setMeasure("");

    ValidateBundle(bundle, issueTextToFind, false);
  }

  private void ValidateBundle(Bundle bundle, String IssueTextToFind, Boolean failOnTextFound)
  {
    logger.info("Beginning Validation. Fail State: {}", failOnTextFound ? "'" + IssueTextToFind + "'" + " Found" : "'" + IssueTextToFind + "'" + " Not Found");

    var lowIssueTextToFind = IssueTextToFind.toLowerCase();

    var errors = GetValidationErrors(bundle);
    var errorDetected = false;

    var allMessages = errors.getIssue().stream().map(i -> {
      return i.getDiagnostics()
              .replaceAll("\\sPatient\\/.+?\\s", " Patient/X ")
              .replaceAll("\\sMeasureReport\\/.+?\\s", " MeasureReport/X ");
    }).collect(Collectors.toList());

    var distinctMessages = allMessages.stream().distinct().collect(Collectors.toList());

    for(var error : distinctMessages) {
      logger.info(error);
      if(error.toLowerCase().contains(lowIssueTextToFind)){
        errorDetected = true;
        //break;
      }
    }

    //If we want to fail the test if the bundle errors contains the correct pattern
    if(failOnTextFound)
      Assert.assertFalse(errorDetected);
      //If we want to fail the test if the bundle errors do not contain the correct pattern
    else
      Assert.assertTrue(errorDetected);
  }

  private OperationOutcome GetValidationErrors(Bundle bundle)
  {
    OperationOutcome oo = this.getValidator().validate(bundle, OperationOutcome.IssueSeverity.ERROR);

    return oo;
  }
}
