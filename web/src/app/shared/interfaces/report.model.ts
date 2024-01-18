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
  tenantId: string;
  tenantName: string;
  cdcOrgId: string;
  reportId: string
  details: string;
};

export interface ReportFilter {
  tenantId?: string;
  status?: 'Draft' | 'Submitted' | string;
  startDate?: string;
  endDate?: string;
  measureIds?: string;
  count?: number;
  page?: number;
}

export interface ReportSummary {
  total: number
  reports: Report[]
}