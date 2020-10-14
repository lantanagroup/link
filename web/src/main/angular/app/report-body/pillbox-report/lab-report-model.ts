import {BaseReportModel} from './base-report-model';

export class LabReportModel extends BaseReportModel {
  patientId: string;
  order: string;
  date: string;
  result: string;

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

  get dateDisplay() {
    return this.getDateDisplay(this.date);
  }
}