import {Component, OnInit} from '@angular/core';
import {formatDate, getFhirNow} from '../helper';
import {ToastService} from '../toast.service';
import {ReportService} from '../services/report.service';
import {QueryReport} from '../model/query-report';
import {StoredReportDefinition} from "../model/stored-report-definition";
import {ReportDefinitionService} from '../services/report-definition.service';

@Component({
  selector: 'app-report-body',
  templateUrl: './report-body.component.html',
  styleUrls: ['./report-body.component.css']
})
export class ReportBodyComponent implements OnInit {
  loading = false;
  sending = false;
  reportGenerated = false;
  today = getFhirNow();
  report: QueryReport = new QueryReport();
  measureConfigs: StoredReportDefinition[] = [];
  evaluateMeasureButtonText: String = 'Select';
  generateReportButtonText: String = 'Generate Report';

  constructor(
      public toastService: ToastService,
      public reportService: ReportService,
      private reportDefinitionService: ReportDefinitionService) {
  }

  onDateSelected() {
    this.today = formatDate(this.report.date);
  }

  async download() {
    try {
      this.report.date = formatDate(this.report.date);
      await this.reportService.download(this.report);
    } catch (ex) {
      this.toastService.showException('Error generating/downloading report', ex);
      return;
    }
  }

  async send() {
    try {
      this.sending = true;
      if (this.report) {
        await this.reportService.send(this.report);
      } else {
        this.toastService.showException('Unable to send blank report', null);
      }
      this.toastService.showInfo('Successfully sent report!');
    } catch (ex) {
      this.toastService.showException('Error sending report', ex);
    } finally {
      this.sending = false;
    }
  }

  get selectedReportTypeDisplay() {
    const found = this.measureConfigs.find(mc => mc.id === this.report.measureId);
    return found ? found.name : 'Select';
  }

  async reload() {
    try {
      this.loading = true;
      this.generateReportButtonText = 'Loading...';

      if (!this.report.date) {
        this.report.date = getFhirNow();
      } else {
        this.report.date = formatDate(this.report.date);
      }

      try {
        const updatedReport = await this.reportService.generate(this.report, false);
        Object.assign(this.report, updatedReport);
      } catch (ex) {
        if (ex.status === 409){
          if (confirm(ex.error.message)) {
            try {
              const updatedReport = await this.reportService.generate(this.report, true);
              Object.assign(this.report, updatedReport);
            }
            catch(ex){
              this.toastService.showException('Error generating report', ex);
            }
          }
        }
        else {
          this.toastService.showException('Error generating report', ex);
        }
        return;
      }

      const keys = Object.keys(this.report);
      for (const key of keys) {
        if (this.report[key] === null) {
          delete this.report[key];
        }
      }

      this.toastService.showInfo('Report generated!');
      this.reportGenerated = true;

    } catch (ex) {
      this.toastService.showException('Error running queries', ex);
    } finally {
      this.loading = false;
      this.generateReportButtonText = 'Generate Report';
    }
  }

  async ngOnInit() {
    // initialize the response date to today by default.
    this.report.date = getFhirNow();
    this.measureConfigs = await this.reportDefinitionService.getReportDefinitions();
  }

  disableGenerateReport() {
    return !this.report.date || !this.report.measureId;
  }
}
