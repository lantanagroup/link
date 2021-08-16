import {Component, Input, OnInit} from '@angular/core';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {ReportService} from '../services/report.service';
import {ReportPatient} from '../model/report-patient';
import {ToastService} from '../toast.service';

@Component({
  templateUrl: './view-line-level.component.html',
  styleUrls: ['./view-line-level.component.css']
})
export class ViewLineLevelComponent implements OnInit {
  @Input() reportId: string;
  public patients: ReportPatient[];
  public loading = false;

  constructor(
      public activeModal: NgbActiveModal,
      private reportService: ReportService,
      private toastService: ToastService) { }

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

  async ngOnInit() {
    await this.refresh();
  }
}
