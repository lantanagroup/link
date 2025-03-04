package com.lantanagroup.link;

public class Constants {
  public static final String DataAbsentReasonExtensionUrl = "http://hl7.org/fhir/StructureDefinition/data-absent-reason";
  public static final String DataAbsentReasonUnknownCode = "unknown";
  public static final String MainSystem = "https://nhsnlink.org";
  public static final String UuidPrefix = "urn:uuid:";
  public static final String CdcOrgIdSystem = "https://www.cdc.gov/nhsn/OrgID";
  public static final String LibraryTypeSystem = "http://terminology.hl7.org/CodeSystem/library-type";
  public static final String LibraryTypeModelDefinitionCode = "model-definition";
  public static final String OrganizationTypeSystem = "http://terminology.hl7.org/CodeSystem/organization-type";
  public static final String ApplicablePeriodExtensionUrl = "http://www.cdc.gov/nhsn/fhirportal/dqm/ig/StructureDefinition/link-patient-list-applicable-period-extension";
  public static final String ReceivedDateExtensionUrl = "http://www.cdc.gov/nhsn/fhirportal/dqm/ig/StructureDefinition/link-received-date-extension";
  public static final String SubjectListMeasureReportProfile = "http://www.cdc.gov/nhsn/fhirportal/dqm/ig/StructureDefinition/subjectlist-measurereport";
  public static final String SubmittingOrganizationProfile = "http://www.cdc.gov/nhsn/fhirportal/dqm/ig/StructureDefinition/nhsn-submitting-organization";
  public static final String SubmittingDeviceProfile = "http://www.cdc.gov/nhsn/fhirportal/dqm/ig/StructureDefinition/nhsn-submitting-device";
  public static final String MeasureScoringExtension = "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/extension-measureScoring";
  public static final String MeasureScoringCodeSystem = "http://terminology.hl7.org/CodeSystem/measure-scoring";
  public static final String MeasureImprovementNotationCodeSystem = "http://terminology.hl7.org/CodeSystem/measure-improvement-notation";
  public static final String LinkDeviceVersionCodeSystem = "http://www.cdc.gov/nhsn/fhirportal/dqm/ig/CodeSystem/codesystem-link-device-version";
  public static final String ReportBundleProfileUrl = "http://www.cdc.gov/nhsn/fhirportal/dqm/ig/StructureDefinition/nhsn-measurereport-bundle";
  public static final String IndividualMeasureReportProfileUrl = "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/indv-measurereport-deqm";
  public static final String CensusProfileUrl = "http://www.cdc.gov/nhsn/fhirportal/dqm/ig/StructureDefinition/poi-list";
  public static final String NationalProviderIdentifierSystemUrl = "http://hl7.org/fhir/sid/us-npi";
  public static final String OperationOutcomeTotalExtensionUrl = "http://nhsnlink.org/oo-total";
  public static final String OperationOutcomeSeverityExtensionUrl = "http://nhsnlink.org/oo-severity";
  public static final String IdentifierSystem = "urn:ietf:rfc:3986";
  public static final String TerminologyEndpointCode = "hl7-fhir-rest";
  public static final String TerminologyEndpointSystem = "http://terminology.hl7.org/CodeSystem/endpoint-connection-type";
  public static final String ConceptMappingExtension = "http://www.cdc.gov/nhsn/fhirportal/dqm/ig/StructureDefinition/link-original-mapped-concept-extension";
  public static final String OriginalElementValueExtension = "http://www.cdc.gov/nhsn/fhirportal/dqm/ig/StructureDefinition/link-original-element-value-extension";
  public static final String OriginalResourceIdExtension = "http://www.cdc.gov/nhsn/fhirportal/dqm/ig/StructureDefinition/link-original-resource-id-extension";
  public static final String BundlingFullUrlFormat = "http://www.cdc.gov/nhsn/fhirportal/dqm/ig/%s";
  public static final String LocationAliasCodeSystem = "https://nhsnlink.org/location-alias";
  public static final String EncounterLocationDisplayCodeSystem = "https://nhsnlink.org/encounter-location-display";

  //Metric taskName Constants
  public static final String TASK_SUBMIT = "submit";
  public static final String TASK_RETRIEVE_PATIENT_DATA = "retrieve-patient-data";
  public static final String TASK_STORE_PATIENT_DATA = "store-patient-data";
  public static final String TASK_MEASURE = "measure";
  public static final String TASK_STORE_MEASURE_REPORT = "store-measure-report";
  public static final String TASK_VALIDATE = "validate";
  public static final String TASK_PATIENT = "patient";


  //Metric category Constants
  public static final String CATEGORY_SUBMISSION = "submission";
  public static final String CATEGORY_VALIDATION = "validation";
  public static final String CATEGORY_TEST = "test";
  public static final String CATEGORY_QUERY = "query";
  public static final String CATEGORY_EVALUATE = "evaluate";
  public static final String CATEGORY_REPORT = "report";
  public static final String CATEGORY_EVENT = "event";

  //Metric report periods
  public static final String WEEKLY_PERIOD = "lastWeek";
  public static final String MONTHLY_PERIOD = "lastMonth";
  public static final String QUARTERLY_PERIOD = "lastQuarter";
  public static final String YEARLY_PERIOD = "lastYear";

  public static final String VALIDATION_ISSUE_TASK = "Validation";
  public static final String VALIDATION_ISSUE_CATEGORY = "Validation Issues";
  public static final String REPORT_GENERATION_TASK = "Report Generation";

  //Submission file names
  public static final String ORGANIZATION_FILE_NAME = "organization.json";
  public static final String DEVICE_FILE_NAME = "device.json";
  public static final String QUERY_PLAN_FILE_NAME = "query-plan.yml";
}
