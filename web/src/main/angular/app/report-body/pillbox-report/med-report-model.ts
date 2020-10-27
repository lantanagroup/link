import {BaseReportModel} from './base-report-model';

export class MedReportModel extends BaseReportModel {
  patientId: string;
  name: string;
  code: string;
  dose: string;
  route: string;
  start: string;
  end: string;

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

  get routeCode() {
    return this.getCodeDisplay(this.route);
  }

  get routeSystem() {
    return this.getCodeSystem(this.route);
  }

  get startDate() {
    return this.getDateDisplay(this.start);
  }

  get endDate() {
    return this.getDateDisplay(this.end);
  }
}