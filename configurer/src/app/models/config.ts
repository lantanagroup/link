export type ConfigTypes = 'api'|'web'|'consumer';

export interface IConfig {
  configType: ConfigTypes;
}

export interface ITerminologyConfig {
  intubationProcedureCodesValueSet?: string;
  covidCodesValueSet?: string;
  ventilatorCodesValueSet?: string;
}

export interface IUserConfig {
  timeZone?: string;
}

export class SwaggerConfig {
  authUrl?: string;
  tokenUrl?: string;
  scope?: string[] = [];
}

export class AgentConfig {
}

export class FHIRSenderConfigWrapper {
  fhir = new FHIRSenderConfig();
}

export class ApiConfig {
  publicAddress: string;
  requireHttps = true;
  measureLocation?: ApiMeasureLocationConfig;
  skipInit = false;
  dataStore  = new DataStore();
  evaluationService?: string;
  terminologyService?: string;
  authJwksUrl?: string;
  issuer?: string;
  downloader?: string;
  sender?: string;
  patientIdResolver?: string;
  documentReferenceSystem = 'urn:ietf:rfc:3986';
  cors = new ApiCorsConfig();
  reportDefs = new ApiReportDefsConfig();
  query?: ApiQueryConfig;
  user?: UserConfig;
  measurePackages: MeasurePackagesConfig[] = [];
  dataProcessor?: { [index: string]: string };
  dataMeasureReportId?: string;
  reportAggregator?: string;
  socketTimeout = 30000;
  events = new ApiConfigEvents();
}

export class ApiConfigEvents {
  beforePatientDataStore?: string[];
  afterReportStore?: string[];
  beforeMeasureResolution?: string[];
  afterMeasureResolution?: string[];
  beforePatientOfInterestLookup?: string[];
  afterPatientOfInterestLookup?: string[];
  beforePatientDataQuery?: string[];
  afterPatientDataQuery?: string[];
  beforeReportStore?: string[];
  beforeMeasureEval?: string[];
  afterPatientDataStore?: string[];
  onRegeneration?: string[]
}

export class ApiCorsConfig {
  allowedOrigins = '*';
  allowedHeaders = '*';
  allowedCredentials = true;
  allowedMethods?: string[] = ['GET', 'POST', 'DELETE', 'PUT', 'OPTIONS'];
}

export class ApiMeasureLocationConfig {
  system?: string;
  value?: string;
  latitude?: number;
  longitude?: number;
}

export class ApiQueryConfig {
  mode: ApiQueryConfigModes;
  url?: string;
  apiKey?: string;
}

export class ApiReportDefsConfig {
  maxRetry = 5;
  retryWait = 5000;
  urls?: ApiReportDefsUrlConfig[];
  auth?: LinkOAuthConfig;
}

export class ApiReportDefsUrlConfig {
  bundleId?: string
  url?: string;
  reportAggregator?: string
}

export class UserConfig {
  timezone?: string;
}

export class DataStore {
  baseUrl: string;
  username: string;
  password: string;
}

export class LinkOAuthConfig {
  credentialMode?: OAuthCredentialModes;
  tokenUrl?: string;
  clientId?: string;
  username?: string;
  password?: string;
  scope?: string;
}

export class ConsumerConfig {
  dataSource = new DataSourceConfig();
  permissions?: Permission[];
  issuer?: string;
  authJwksUrl?: string;
}

export class ConceptMapConfigWrapper {
  conceptMaps: ConceptMapConfig[];
}

export class ConceptMapConfig {
  conceptMapId: string;
  fhirPathContexts: string[] = [];
}

export class MeasurePackagesConfig {
  id: string;
  bundleIds: string[];
}

export class DataSourceConfig {
  url = 'jdbc:h2:file:./target/database/h2';
  username = 'sa';
  password?: string;
  driverClassName = 'org.h2.Driver';
  hibernateDialect = 'org.hibernate.dialect.H2Dialect';
}

export class Permission {
  resourceType?: string;
  roles?: Role[];
}

export class Role {
  name?: string;
  permission?: string[];
}

export class QueryConfig {
  requireHttps?: boolean;
  apiKey?: string;
  queryClass?: string;
  allowedRemote?: string[];
  proxyAddress?: string;
  authClass?: string;
  auth?: QueryAuth;
}

export class QueryAuth {
  azure?: QueryAuthAzure;
  epic?: QueryAuthEpic;
  cerner?: QueryAuthCerner;
  basic?: QueryAuthBasic;
  token?: QueryAuthToken;
}

export class QueryAuthToken {
  token: string;
}

export class QueryAuthEpic {
  key: string;
  tokenUrl: string;
  clientId: string;
  audience: string;
}

export class QueryAuthCerner {
  tokenUrl: string;
  clientId: string;
  secret: string;
  scopes: string;
}

export class QueryAuthBasic {
  username: string;
  password: string;
}

export class QueryAuthAzure {
  tokenUrl: string;
  clientId: string;
  secret: string;
  resource: string;
}

export class USCoreConfig {
  fhirServerBase?: string;
  patientResourceTypes?: string[];
  otherResourceTypes?: string[];
  queryParameters?: Map<string, any>;
  parallelPatients = 10;
}

export class QueryResourceParams {
  resourceType?: string;
  parameters?: Parameter[];
}

export class Parameter {
  name?: string;
  values?: string[];
}

export class FHIRSenderConfig {
  authConfig?: LinkOAuthConfig;
  url?: string;
}

export class BundlerConfig {
  bundleType?: string
  includeCensuses: boolean;
  mergeCensuses: boolean;
  includeIndividualMeasureReports: boolean;
  reifyLineLevelResources: boolean;
  promoteLineLevelResources: boolean;
}


export class THSAConfig {
  dataMeasureReportId?: string;
}

export type OAuthCredentialModes = 'Client'|'Password'
export type ApiQueryConfigModes = 'Local' | 'Remote'
