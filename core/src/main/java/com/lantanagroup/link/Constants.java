package com.lantanagroup.link;

public class Constants {
  public static final String DataAbsentReasonExtensionUrl = "http://hl7.org/fhir/StructureDefinition/data-absent-reason";
  public static final String DataAbsentReasonUnknownCode = "unknown";
  public static final String MainSystem = "https://nhsnlink.org";
  public static final String UuidPrefix = "urn:uuid:";
  public static final String CdcOrgIdSystem = "https://www.cdc.gov/nhsn/OrgID";
  public static final String OrganizationTypeSystem = "http://terminology.hl7.org/CodeSystem/organization-type";
  public static final String ApplicablePeriodExtensionUrl = "http://lantanagroup.com/fhir/nhsn-measures/StructureDefinition/link-patient-list-applicable-period-extension";
  public static final String ReceivedDateExtensionUrl = "http://lantanagroup.com/fhir/nhsn-measures/StructureDefinition/link-received-date-extension";
  public static final String SubjectListMeasureReportProfile = "http://lantanagroup.com/fhir/nhsn-measures/StructureDefinition/subjectlist-measurereport";
  public static final String SubmittingOrganizationProfile = "http://lantanagroup.com/fhir/nhsn-measures/StructureDefinition/nhsn-submitting-organization";
  public static final String MeasureScoringExtension = "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/extension-measureScoring";
  public static final String MeasureImprovementNotationCodeSystem = "http://terminology.hl7.org/CodeSystem/measure-improvement-notation";
  public static final String QiCorePatientProfileUrl = "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-patient";
  public static final String UsCoreEncounterProfileUrl = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-encounter";
  public static final String UsCoreMedicationRequestProfileUrl = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-medicationrequest";
  public static final String UsCoreMedicationProfileUrl = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-medication";
  public static final String UsCoreConditionProfileUrl = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-condition";
  public static final String UsCoreObservationProfileUrl = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-observation-lab";
  public static final String ReportBundleProfileUrl = "http://lantanagroup.com/fhir/nhsn-measures/StructureDefinition/nhsn-measurereport-bundle";
  public static final String IndividualMeasureReportProfileUrl = "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/indv-measurereport-deqm";
  public static final String CensusProfileUrl = "http://lantanagroup.com/fhir/nhsn-measures/StructureDefinition/poi-list";
  public static final String NationalProviderIdentifierSystemUrl = "http://hl7.org.fhir/sid/us-npi";
  public static final String IdentifierSystem = "urn:ietf:rfc:3986";
  public static final String TerminologyEndpointCode = "hl7-fhir-rest";
  public static final String TerminologyEndpointSystem = "http://terminology.hl7.org/CodeSystem/endpoint-connection-type";
  public static final String ConceptMappingExtension = "https://www.lantanagroup.com/fhir/StructureDefinition/mapped-concept";
  public static final String OriginalElementValueExtension = "http://lantanagroup.com/fhir/nhsn-measures/StructureDefinition/link-original-element-value-extension";
  public static final String OriginalResourceIdExtension = "http://lantanagroup.com/fhir/nhsn-measures/StructureDefinition/link-original-resource-id-extension";
  public static final String BundlingFullUrlFormat = "http://lantanagroup.com/fhir/nhsn-measures/%s";
}
