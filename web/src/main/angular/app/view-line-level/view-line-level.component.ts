import {Component, Input, OnInit} from '@angular/core';
import {NgbActiveModal, NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {ReportService} from '../services/report.service';
import {ReportPatient} from '../model/report-patient';
import {ToastService} from '../toast.service';
import {ViewPatientComponent} from "../view-patient/view-patient.component";
import {ExcludedPatientModel} from "../model/ExcludedPatientModel";
import {ReportPatientsIncludedExcluded} from "../model/report-patients-included-excluded";

@Component({
  templateUrl: './view-line-level.component.html',
  styleUrls: ['./view-line-level.component.css']
})
export class ViewLineLevelComponent implements OnInit {
  @Input() reportId: string

  public patients: ReportPatient[];
  public excludedPatients: ExcludedPatientModel[] = [];
  public reportPatientsIncludedExcluded: ReportPatientsIncludedExcluded[] = [];
  public loading = false;
  public canSave = false;
  public codes = [
    {
      code: 'FNCIEMR',
      display: 'Findings not captured in EMR',
      system: 'system1'
    },
    {
      code: 'FFFMROEUFAC',
      display: 'Findings found from manual review of EMR unavailable from automated capture',
      system: 'system2'
    },
    {
      code: 'EIDIIACFE',
      display: 'Error in data included in automated capture from EMR',
      system: 'system1'
    }
  ];

  public constructor(
      public  activeModal: NgbActiveModal,
      private reportService: ReportService,
      private modal: NgbModal,
      private toastService: ToastService) {
  }

  public async refresh() {
    this.loading = true;
    try {
      this.patients = await this.reportService.getReportPatients(this.reportId);
      for (let patient of this.patients) {
        let patientsIncludedExcluded = {...patient, excluded: false, text: '', coding: ''};
        this.reportPatientsIncludedExcluded.push(patientsIncludedExcluded);
      }
      this.excludedPatients = [];
    } catch (ex) {
      this.toastService.showException('Error loading line-level data', ex);
    } finally {
      this.loading = false;
    }
  }

  excludePatient(patient: ReportPatient) {
    const foundPatient = this.reportPatientsIncludedExcluded.filter(mypatient => mypatient.id === patient.id)[0];
    foundPatient.excluded = true;
    this.validate();
  }

  reIncludePatient(patient: ReportPatient) {
    const foundPatient = this.reportPatientsIncludedExcluded.filter(mypatient => mypatient.id === patient.id)[0];
    foundPatient.excluded = false;
    foundPatient.text = '';
    foundPatient.coding = '';
    this.validate();
  }

  viewPatientData(patientId) {
    const modalRef = this.modal.open(ViewPatientComponent, {size: 'xl'});
    modalRef.componentInstance.patientId = patientId;
    modalRef.componentInstance.reportId = this.reportId;
  }

  anyPatientExcluded() {
    const patient = this.reportPatientsIncludedExcluded.find(patient => patient.excluded == true);
    if (patient !== undefined) {
      return true;
    }
    return false;
  }

  validate() {
    this.canSave = true;
    for (let patient of this.reportPatientsIncludedExcluded) {
      if (patient.excluded && patient.text === '' && patient.coding === '' || patient.coding === 'other' && patient.text === '') {
        this.canSave = false;
        break;
      }
    }
  }

  save() {
    if (this.canSave) {

      //TO-DO
    }
  }

  async ngOnInit() {
    await this.refresh();
  }

  addPatientToExcludedList(patient) {
    const coding = this.codes.find(code => code.code === patient.coding);
    const codeableConcept = {coding: [{system: "test", code: patient.coding, display: "tesT"}], text: patient.text};
    this.excludedPatients.push({patientId: patient.id, reason: codeableConcept});
  }

  private generateExcludedPatientsList() {
    this.excludedPatients = [];
    for (let patient of this.reportPatientsIncludedExcluded) {
      if (patient.excluded) {
        this.addPatientToExcludedList(patient);
      }
    }
  }
}
