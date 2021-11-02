import {ReportPatient} from "./report-patient";

export class ReportPatientsIncludedExcluded extends ReportPatient {
  excludePending: boolean;
  text: string;
  coding: string;
}
