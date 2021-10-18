import {ReportPatient} from "./report-patient";

export class ReportPatientsIncludedExcluded extends ReportPatient {
  excluded: boolean;
  text: string;
  coding: string;
}
