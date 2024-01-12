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
  name: string
  contexts: string[] 
}