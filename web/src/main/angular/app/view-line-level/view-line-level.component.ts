import {Component, Input, OnInit} from '@angular/core';
import {NgbActiveModal, NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {ReportService} from '../services/report.service';
import {ReportPatient} from '../model/report-patient';
import {ToastService} from '../toast.service';
import {ViewPatientComponent} from "../view-patient/view-patient.component";
import {ExcludedPatientModel} from "../model/ExcludedPatientModel";
import {ReportPatientsIncludedExcluded} from "../model/report-patients-included-excluded";
import {CodeableConcept} from "../model/fhir";

@Component({
  templateUrl: './view-line-level.component.html',
  styleUrls: ['./view-line-level.component.css']
})
export class ViewLineLevelComponent implements OnInit {
  @Input() reportId: string
  loading = false;
  patients: ReportPatient[];
  excludedPatients: ExcludedPatientModel[] = [];
  reportPatientsIncludedExcluded: ReportPatientsIncludedExcluded[] = [];
  saving = false;
  canSave = false;
  excludedButtonText = 'Exclude Selected';

  codes = [
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
        let patientsIncludedExcluded = {...patient, excludePending: false, text: '', coding: ''};
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
    foundPatient.excludePending = true;
    this.validate();
  }

  reIncludePatient(patient: ReportPatient) {
    const foundPatient = this.reportPatientsIncludedExcluded.filter(mypatient => mypatient.id === patient.id)[0];
    foundPatient.excludePending = false;
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
      if (patient.excludePending && (patient.coding === '' || (patient.coding === 'other' && patient.text.trim() === ''))) {
        this.canSave = false;
        break;
      }
    }
  }

  async save() {
    if (this.canSave) {
      this.excludedButtonText = 'Excluding...';
      this.saving = true;
      try {
        this.generateExcludedPatientsList();
        await this.reportService.excludePatients(this.reportId, this.excludedPatients);
        this.reportPatientsIncludedExcluded = this.reportPatientsIncludedExcluded.filter(ar => !this.excludedPatients.find(rm => (rm.patientId === ar.id)));
        this.excludedPatients = [];
        this.excludedButtonText = 'Excluded';
      } catch (ex) {
        this.toastService.showException('Error excluding patients', ex);
      } finally {
        this.excludedButtonText = "Exclude Selected";
        this.canSave = true;
        this.saving = false;
        this.reportPatientsIncludedExcluded = [];
        await this.refresh();
      }
    }
  }

  getNoPatientsIncludedInReport() {
    return this.reportPatientsIncludedExcluded.filter(patient => patient.excluded == false).length;
  }

  async ngOnInit() {
    await this.refresh();
  }

  addPatientToExcludedList(patient) {
    const codeableConcept = new CodeableConcept();
    const code = this.codes.find(code => code.code === patient.coding);
    if (code !== undefined) {
      codeableConcept.coding = [{system: code.system, code: patient.coding, display: code.display}]
    } else {
      codeableConcept.text = patient.text.trim();
    }
    this.excludedPatients.push({patientId: patient.id, reason: codeableConcept});
  }

  private generateExcludedPatientsList() {
    this.excludedPatients = [];
    for (let patient of this.reportPatientsIncludedExcluded) {
      if (patient.excludePending) {
        this.addPatientToExcludedList(patient);
      }
    }
  }

}
