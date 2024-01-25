export interface TenantMeasure {
  id: string;
  shortName: string;
  longName: string;
}

export interface Tenant {
  id: string;
  name: string;
  nhsnOrgId: string;
  lastSubmissionId: string;
  lastSubmissionDate: string;
  measures: TenantMeasure[];
}

export interface TenantSummary {
  total: number
  tenants: Tenant[]
}

export interface TenantConceptMap {
  id: string
  name?: string
  contexts: string[]
  map?: any // todo: not sure what this looks like 
}


/// for tenant data,will clean up
export interface Normalization {
  name: string
  value: string
}

interface ReferencesConfig {
  operationType: string
  paged: number
}

interface ParametersConfig {
  format: string | null,
  ids: string | null,
  literal: string | null,
  name: string | null,
  paged: number,
  variable: string | null
}

 interface TypedQueryPlan {
  resourceType: string
  parameters: ParametersConfig[]
  references: ReferencesConfig
  earlyExit: boolean
}

export interface QueryPlan {
  measureId?: string
  lookback: string
  initial: TypedQueryPlan[]
  supplemental: TypedQueryPlan[]
}

export interface QueryPlans {
  [key: string]: QueryPlan
}



// Full Details

interface Bundle {
  address?: string
  bundleType?: string
  email?: string
  includeCensuses?: boolean
  includesQueryPlans?: boolean
  mergeCensuses?: boolean
  name: string
  npi?: string
  phone?: string
  promoteLineLevelResources?: boolean
}

export interface Events {
  beforeMeasureResolution?: string[] | null
  afterMeasureResolution?: string[] | null
  onRegeneration?: string[] | null
  beforePatientOfInterestLookup?: string[] | null
  afterPatientOfInterestLookup?: string[] | null
  beforePatientDataQuery?: string[] | null
  afterPatientResourceQuery?: string[] | null
  afterPatientDataQuery?: string[] | null
  afterApplyConceptMaps?: string[] | null
  beforePatientDataStore?: string[] | null
  afterPatientDataStore?: string[] | null
  beforeMeasureEval?: string[] | null
  afterMeasureEval?: string[] | null
  beforeReportStore?: string[] | null
  afterReportStore?: string[] | null
  beforeBundling?: string[] | null
  afterBundling?: string[] | null
}

interface Auth {
  username?: string
  password?: string
  apiKey?: string
  token?: string
  tokenUrl?: string
  clientId?: string
  secret?: string
  resource?: string
  scopes?: string
  key?: string
  audience?: string
}

export enum AuthClass {
  BasicAuth = 'com.lantana.group.link.query.auth.BasicAuth',
  BasicAuthAndApiKey = 'com.lantana.group.link.query.auth.BasicAuthAndApiKey',
  AzureAuth = 'com.lantana.group.link.query.auth.AzureAuth',
  EpicAuth = 'com.lantana.group.link.query.auth.EpicAuth',
  TokenAuth = 'com.lantana.group.link.query.auth.TokenAuth',
  CernerAuth = 'com.lantana.group.link.query.auth.CernerAuth',
}

export type AuthType =
  'BasicAuth' | 
  'BasicAuthAndApiKey' | 
  'AzureAuth' | 
  'EpicAuth' | 
  'CernerAuth' | 
  'TokenAuth';

export enum NormalizationClass {
  codeSystemCleanup = 'com.lantanagroup.link.events.CodeSystemCleanup',
  containedResourceCleanup = 'com.lantanagroup.link.events.ContainedResourceCleanup',
  copyLocationIdentifierToType = 'com.lantanagroup.link.events.CopyLocationIdentifierToType',
  encounterStatusTransformer = 'com.lantanagroup.link.events.EncounterStatusTransformer',
  fixPeriodDates = 'com.lantanagroup.link.events.FixPeriodDates',
  fixResourceId = 'com.lantanagroup.link.events.FixResourceId',
  patientDataResourceFilter = 'com.lantanagroup.link.events.PatientDataResourceFilter'
}

interface FHIRQuery {
  fhirServerBase: string
  authClass?: AuthClass
  parallelPatients: number
  basicAuth?: Auth
  basicAuthAndApiKey?: Auth
  tokenAuth?: Auth
  azureAuth?: Auth
  epicAuth?: Auth
  cernerAuth?: Auth
  queryPlans: QueryPlans
}

interface QueryList {
  listIds: string[]
  measureId: string[]
}

interface QueryLists {
  fhirServerBase: string
  lists: QueryList[]
}

export interface Schedule {
  cron: string
  measureIds: string[]
  regenerateIfExists: boolean
  reportingPeriodMethod: "LastMonth" | "LastWeek" | "CurrentMonth" | "CurrentWeek"
}

interface Scheduling {
  bulkDataCron?: string
  bulkDataFollowUpCron?: string
  dataRetentionCheckCron?: string
  generateAndSubmitReports?: Schedule[]
  queryPatientListCron?: string
}

export interface TenantDetails {
  bulkWaitTimeInMilliseconds?: string | null
  bundling?: Bundle
  cdcOrgId: string
  connectionString?: string
  description?: string | null
  events?: Events
  fhirQuery?: FHIRQuery
  id?: string
  name?: string
  queryList?: QueryLists
  retentionPeriod?: string
  scheduling?: Scheduling
  timeZoneId?: string
}