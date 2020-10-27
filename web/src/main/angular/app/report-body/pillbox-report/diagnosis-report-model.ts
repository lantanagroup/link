import {BaseReportModel} from './base-report-model';

export class DiagnosisReportModel extends BaseReportModel {
  patientId: string;
  code: string;

  constructor(data?: any) {
    super();
    if (data) Object.assign(this, data);
  }

  get patientIdCode() {
    return this.getCodeDisplay(this.patientId);
  }

  get patientIdSystem() {
    return this.getCodeSystem(this.patientId);
  }

  get codeDisplay() {
    return this.getCodeDisplay(this.code);
  }

  get codeSystem() {
    return this.getCodeSystem(this.code);
  }
}