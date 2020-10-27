import {Component, Input, OnInit} from '@angular/core';
import {IReportPlugin} from '../report-plugin';
import {QueryReport} from '../../model/query-report';
import {PatientReportModel} from './patient-report-model';
import {MedReportModel} from './med-report-model';
import {LabReportModel} from './lab-report-model';
import {DiagnosisReportModel} from './diagnosis-report-model';

@Component({
  templateUrl: './pillbox-report.component.html',
  styleUrls: ['./pillbox-report.component.css']
})
export class PillboxReportComponent implements OnInit, IReportPlugin {
  @Input() report: QueryReport;
  patientData: PatientReportModel[] = [];
  medsData: MedReportModel[] = [];
  labsData: LabReportModel[] = [];
  dxData: DiagnosisReportModel[] = [];

  constructor() { }

  ngOnInit(): void {
    this.report.questions = [
        'patients',
        'meds',
        'labs',
        'dx'
    ];
  }

  refreshed() {
    this.patientData = [];
    this.medsData = [];
    this.labsData = [];
    this.dxData = [];

    if (this.report.answers['patients']) {
      this.patientData = this.report.answers['patients'].map(patient => new PatientReportModel(patient));
    }

    if (this.report.answers['meds']) {
      this.medsData = this.report.answers['meds'].map(med => new MedReportModel(med));
    }

    if (this.report.answers['labs']) {
      this.labsData = this.report.answers['labs'].map(lab => new LabReportModel(lab));
    }

    if (this.report.answers['dx']) {
      this.dxData = this.report.answers['dx'].map(dx => new DiagnosisReportModel(dx));
    }
  }
}
