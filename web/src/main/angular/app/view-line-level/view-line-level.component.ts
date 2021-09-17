import {Component, Input, OnInit} from '@angular/core';
import {NgbActiveModal, NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {ReportService} from '../services/report.service';
import {ReportPatient} from '../model/report-patient';
import {ToastService} from '../toast.service';
import {ViewPatientComponent} from "../view-patient/view-patient.component";

@Component({
  templateUrl: './view-line-level.component.html',
  styleUrls: ['./view-line-level.component.css']
})
export class ViewLineLevelComponent implements OnInit {
  @Input() reportId: string

  public patients: ReportPatient[];
  public loading = false;

  constructor(
      public  activeModal: NgbActiveModal,
      private reportService: ReportService,
      private modal: NgbModal,
      private toastService: ToastService) {
  }

  public async refresh() {
    this.loading = true;

    try {
      this.patients = await this.reportService.getReportPatients(this.reportId);
    } catch (ex) {
      this.toastService.showException('Error loading line-level data', ex);
    } finally {
      this.loading = false;
    }
  }

  viewPatientData(patientId) {
    const modalRef = this.modal.open(ViewPatientComponent, {size: 'xl'});
    modalRef.componentInstance.patientId = patientId;
    modalRef.componentInstance.reportId = this.reportId;
  }

  async ngOnInit() {
    await this.refresh();
  }
}
