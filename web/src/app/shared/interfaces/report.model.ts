export interface Report {
  id: string;
  measureIds: string[];
  periodStart: string;
  periodEnd: string;
  status: string;
  submittedTime: string;
  generatedTime: string;
  version: string;
  patientLists: string[];
  aggregates: string[];
  tenantName: string;
  nhsnOrgId: string;
  reportId: string;
  activity: string;
  details: string;
};

export interface ReportFilter {
  tenantId?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
  measureIds?: string;
  count?: number;
  page?: number;
}
