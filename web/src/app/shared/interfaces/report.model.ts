export interface Report {
  id: string;
  measureId: string[];
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
