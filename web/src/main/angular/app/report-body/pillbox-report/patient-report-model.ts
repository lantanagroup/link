import {BaseReportModel} from './base-report-model';

export class PatientReportModel extends BaseReportModel {
  facilityId: string;
  censusId: string;
  patientId: string;
  admitDate: string;
  dischargeDate: string;
  age: string;
  sex: string;
  race: string;
  ethnicity: string;
  chiefComplaint: string;
  primaryDx: string;
  location: string;
  disposition: string;

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

  get raceCode() {
    return this.getCodeDisplay(this.race);
  }

  get raceSystem() {
    return this.getCodeSystem(this.race);
  }

  get ethnicityCode() {
    return this.getCodeDisplay(this.ethnicity);
  }

  get ethnicitySystem() {
    return this.getCodeSystem(this.ethnicity);
  }

  get chiefComplaintCode() {
    return this.getCodeDisplay(this.chiefComplaint);
  }

  get chiefComplaintSystem() {
    return this.getCodeSystem(this.chiefComplaint);
  }

  get primaryDxCode() {
    return this.getCodeDisplay(this.primaryDx);
  }

  get primaryDxSystem() {
    return this.getCodeSystem(this.primaryDx);
  }

  get dispositionCode() {
    return this.getCodeDisplay(this.disposition);
  }

  get dispositionSystem() {
    return this.getCodeSystem(this.disposition);
  }
}