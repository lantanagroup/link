package com.lantanagroup.link.validation;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.db.model.tenant.Validation;
import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
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
  public void testPerformance() throws IOException {
    Bundle bundle = this.getBundle("large-submission-example.json");
    OperationOutcome oo = this.getValidator().validate(bundle, OperationOutcome.IssueSeverity.INFORMATION);

    logger.info("Issues:");
    oo.getIssue().forEach(i -> {
      logger.info("{} - {} - {}", i.getSeverity(), i.getLocation(), i.getDiagnostics());
    });
  }

//  @Test
//  public void validateUsCore_Location_Name() throws IOException {
//    var bundle = this.getBundle("LINK_888_Location.json");
//
//    var resources = bundle.getEntry().stream()
//            .map(Bundle.BundleEntryComponent::getResource)
//            .filter(r -> r instanceof Location)
//            .map(r -> (Location) r)
//            .collect(Collectors.toList());
//
//    var issueTextToFind = "location";
//
//    resources.stream().forEach(l -> l.setNameElement(null));
//    String str = null;
//    resources.stream().forEach(l -> l.setId(str));
//    resources.stream().forEach(l -> l.setStatus(null));
//
//    ValidateBundle(bundle, issueTextToFind, false);
//  }

  @Test
  public void validateUsCore_Patient_Name() throws IOException {
    var bundle = this.getBundle("large-submission-example2.json");

    var resource = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof Patient)
            .map(r -> (Patient) r)
            .findFirst();

    var issueTextToFind = "Patient.name: minimum required = 1, but only found 0";

    resource.get().getName().clear();

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  public void validateUsCore_Patient_Identifier() throws IOException {
    var bundle = this.getBundle("large-submission-example2.json");

    var resource = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof Patient)
            .map(r -> (Patient) r)
            .findFirst();

    var issueTextToFind = "Patient.identifier: minimum required = 1, but only found 0";

    resource.get().getIdentifier().clear();

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  public void validateUsCore_Patient_Gender() throws IOException {

    var bundle = this.getBundle("large-submission-example.json");

    var resource = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof Patient)
            .map(r -> (Patient) r)
            .findFirst();

    var issueTextToFind = "The value provided ('?') is not in the value set 'AdministrativeGender'";

    resource.get().setGender(Enumerations.AdministrativeGender.NULL);

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  public void validateUsCore_Encounter_Class() throws IOException {

    var bundle = this.getBundle("new-sira-bundle.json");

    var encounter = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof Encounter)
            .map(r -> (Encounter) r)
            .findFirst();

    var issueTextToFind = "Encounter.class: minimum required = 1, but only found 0";

    encounter.get().setClass_(null);

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  public void validateUsCore_Encounter_Class_Modify() throws IOException {

    var bundle = this.getBundle("new-sira-bundle.json");

    var resource = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof Encounter)
            .map(r -> (Encounter) r)
            .findFirst();

    var issueTextToFind = "Unknown Code";

    resource.get().getClass_().setCode("Bogus Code To Fail");

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  public void validateUsCore_Encounter_Status() throws IOException {

    var bundle = this.getBundle("new-sira-bundle.json");

    var resource = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof Encounter)
            .map(r -> (Encounter) r)
            .findFirst();

    var issueTextToFind = "Encounter.status: minimum required = 1, but only found 0";

    resource.get().setStatus(null);

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  public void validateUsCore_Encounter_Type() throws IOException {

    var bundle = this.getBundle("new-sira-bundle.json");

    var resources = bundle.getEntry().stream()
            .filter(r -> r.getResource() instanceof Encounter)
            .collect(Collectors.toList());

    var issueTextToFind = "Encounter.type: minimum required = 1, but only found 0 ";

    for(var resource : resources)
    {
      ((Encounter)resource.getResource()).setType(null);
    }

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  public void validateUsCore_MedicationRequest_Status() throws IOException {

    var bundle = this.getBundle("LINK_887_MedicationRequest.json");

    var resource = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof MedicationRequest)
            .map(r -> (MedicationRequest) r)
            .findFirst();

    var issueTextToFind = "MedicationRequest.status: minimum required = 1, but only found 0";

    resource.get().setStatus(null);

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  public void validateUsCore_MedicationRequest_Status_SetNullValue() throws IOException {

    var bundle = this.getBundle("LINK_887_MedicationRequest.json");

    var medicationRequest = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof MedicationRequest)
            .map(r -> (MedicationRequest) r)
            .findFirst();

    var issueTextToFind = "The value provided ('?') is not in the value set 'Medicationrequest  status";

    medicationRequest.get().setStatus(MedicationRequest.MedicationRequestStatus.NULL);

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  public void validateUsCore_MedicationRequest_IntentCode() throws IOException {

    var bundle = this.getBundle("LINK_887_MedicationRequest.json");

    var resource = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof MedicationRequest)
            .map(r -> (MedicationRequest) r)
            .findFirst();

    var issueTextToFind = "MedicationRequest.intent: minimum required = 1, but only found 0";

    resource.get().setIntent(null);

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  public void validateUsCore_MedicationRequest_IntentCode_SetNullValue() throws IOException {

    var bundle = this.getBundle("LINK_887_MedicationRequest.json");

    var resource = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof MedicationRequest)
            .map(r -> (MedicationRequest) r)
            .findFirst();

    var issueTextToFind = "The value provided ('?') is not in the value set 'Medication request  intent'";

    resource.get().setIntent(MedicationRequest.MedicationRequestIntent.NULL);

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  public void validateUSCore_Encounter_Subject() throws IOException {
    var bundle = this.getBundle("new-sira-bundle.json");

    var resource = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof Encounter)
            .map(r -> (Encounter) r)
            .findFirst();

    var issueTextToFind = "Encounter.subject: minimum required = 1, but only found 0";

    resource.get().setSubject(null);

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  public void validateQICore_Encounter_Diagnosis_Condition() throws IOException {
    var bundle = this.getBundle("new-sira-bundle.json");

    var encounter = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof Encounter)
            .filter(e -> !((Encounter) e).getDiagnosis().isEmpty())
            .map(r -> (Encounter) r)
            .findFirst();

    var issueTextToFind = "Encounter.diagnosis.condition: minimum required = 1, but only found 0";

    encounter.get().getDiagnosis().stream().findFirst().get().setCondition(null);

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  public void validateQICore_Observation_Subject() throws IOException {
    var bundle = this.getBundle("new-sira-bundle.json");

    var resource = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof Observation)
            .map(r -> (Observation) r)
            .findFirst();

    var issueTextToFind = "Observation.subject: minimum required = 1, but only found 0";

    resource.get().setSubject(null);

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  public void validateQICore_Observation_Category() throws IOException {
    var bundle = this.getBundle("new-sira-bundle.json");

    var resource = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof Observation)
            .filter(r -> ((Observation) r).hasCategory())
            .map(r -> (Observation) r)
            .findFirst();

    var issueTextToFind = "Observation.category: minimum required = 1, but only found 0";

    resource.get().setCategory(null);

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  public void validateNhsnMeasure_Organization() throws IOException {

    var bundle = this.getBundle("large-submission-example2.json");

    var resource = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof Organization)
            .map(r -> (Organization)r)
            .findFirst();

    var issueTextToFind = "Bundle.entry:organization: minimum required = 1, but only found 0";

    resource.get().setName("");
    resource.get().setId("");

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  public void validateNhsnMeasure_Location() throws IOException {

    var bundle = this.getBundle("new-sira-bundle.json");

    var resource = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof Encounter)
            .map(r -> (Encounter) r)
            .findFirst();

    var issueTextToFind = "Encounter.location.location: minimum required = 1, but only found 0";

    if(!resource.get().getLocation().isEmpty()) {
      for(var encLoc : resource.get().getLocation()){
        encLoc.setLocation(null);
      }
    }

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  public void validateNhsnMeasure_LocationStatus() throws IOException {

    var bundle = this.getBundle("new-sira-bundle.json");

    var resource = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof Encounter)
            .map(r -> (Encounter) r)
            .findFirst();

    var issueTextToFind = "The value provided ('?') is not in the value set 'EncounterLocationStatus'";

    if(!resource.get().getLocation().isEmpty()) {
      for(var encLoc : resource.get().getLocation()){
        encLoc.setStatus(Encounter.EncounterLocationStatus.NULL);
      }
    }

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  public void validateNhsnMeasure_Observation() throws IOException {

    var bundle = this.getBundle("new-sira-bundle.json");

    var resources = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof Observation)
            .map(r -> (Observation) r)
            .collect(Collectors.toList());

    var issueTextToFind = "Observation.code: minimum required = 1, but only found 0";

    for(var obs: resources)
    {
      obs.setId("");
      obs.setCode(null);
      obs.setEncounter(null);
    }

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  public void validateNhsnMeasure_MeasureReport_Measure() throws IOException {

    var bundle = this.getBundle("single-submission-example.json");

    var measureReport = bundle.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(r -> r instanceof MeasureReport)
            .map(r -> (MeasureReport) r)
            .findFirst();

    var issueTextToFind = "MeasureReport.measure: minimum required = 1, but only found 0";

    measureReport.get().setMeasure("");

    ValidateBundle(bundle, issueTextToFind, false);
  }

  @Test
  public void validateNhsnMeasure_MeasureReport_SummaryReport() throws IOException {

    var bundle = this.getBundle("single-submission-example.json");

    var resources = bundle.getEntry().stream()
            .filter(r -> r.getResource() instanceof MeasureReport)
            .collect(Collectors.toList());

    var issueTextToFind = "Bundle.entry:poi-list: minimum required = 1, but only found 0";

    for(var resource : resources)
    {
      bundle.getEntry().remove(resource);
    }

    ValidateBundle(bundle, issueTextToFind, false);
  }

  private boolean ValidateBundle(Bundle bundle, String IssueTextToFind, Boolean failOnTextFound)
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
        break;
      }
    }

    //If we want to fail the test if the bundle errors contains the correct pattern
    if(failOnTextFound)
      Assert.assertFalse(errorDetected);
      //If we want to fail the test if the bundle errors do not contain the correct pattern
    else
      Assert.assertTrue(errorDetected);

    return errorDetected;
  }

  private OperationOutcome GetValidationErrors(Bundle bundle)
  {
    OperationOutcome oo = this.getValidator().validate(bundle, OperationOutcome.IssueSeverity.ERROR);

    return oo;
  }
}
