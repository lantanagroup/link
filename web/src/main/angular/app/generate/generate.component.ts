import {Component, OnInit} from '@angular/core';
import {formatDate, getFhirNow} from '../helper';
import {StoredReportDefinition} from '../model/stored-report-definition';
import {ToastService} from '../toast.service';
import {ReportService} from '../services/report.service';
import {ReportDefinitionService} from '../services/report-definition.service';
import {Router} from '@angular/router';

@Component({
  selector: 'nandina-generate',
  templateUrl: './generate.component.html',
  styleUrls: ['./generate.component.css']
})
export class GenerateComponent implements OnInit {
  loading = false;
  sending = false;
  reportGenerated = false;
  today = getFhirNow();
  criteria: {
    reportDefId?: string,
    periodStart?: string
  } = {};
  measureConfigs: StoredReportDefinition[] = [];
  evaluateMeasureButtonText: String = 'Select';
  generateReportButtonText: String = 'Generate Report';

  constructor(
      public toastService: ToastService,
      public reportService: ReportService,
      private reportDefinitionService: ReportDefinitionService,
      private router: Router) {
  }

  onDateSelected() {
    this.today = formatDate(this.criteria.periodStart);
  }

  get selectedReportTypeDisplay() {
    const found = this.measureConfigs.find(mc => mc.id === this.criteria.reportDefId);
    return found ? found.name : 'Select';
  }

  async reload() {
    try {
      this.loading = true;
      this.generateReportButtonText = 'Loading...';

      if (!this.criteria.periodStart) {
        this.criteria.periodStart = getFhirNow();
      } else {
        this.criteria.periodStart = formatDate(this.criteria.periodStart);
      }

      // TODO: calculate periodEnd
      const periodStart = this.criteria.periodStart;
      const periodEndDate = new Date(Date.parse(periodStart));
      periodEndDate.setDate(periodEndDate.getDate() + 1);
      const periodEnd = periodEndDate.toISOString().substring(0, 10);

      try {
        const generateResponse = await this.reportService.generate(this.criteria.reportDefId, periodStart, periodEnd);
        await this.router.navigate(['review', generateResponse.reportId]);
      } catch (ex) {
        if (ex.status === 409){
          if (confirm(ex.error.message)) {
            try {
              const generateResponse = await this.reportService.generate(this.criteria.reportDefId, this.criteria.periodStart, periodEnd, true);
              await this.router.navigate(['review', generateResponse.reportId]);
            } catch(ex) {
              this.toastService.showException('Error generating report', ex);
            }
          }
        } else {
          this.toastService.showException('Error generating report', ex);
        }
        return;
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
    this.criteria.periodStart = getFhirNow();
    this.measureConfigs = await this.reportDefinitionService.getReportDefinitions();
  }

  disableGenerateReport() {
    return !this.criteria.periodStart || !this.criteria.reportDefId;
  }
}
