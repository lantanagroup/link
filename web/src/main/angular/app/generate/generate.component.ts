import {Component, OnInit} from '@angular/core';
import {formatDateToISO, getFhirDate, getFhirNow} from '../helper';
import {StoredReportDefinition} from '../model/stored-report-definition';
import {ToastService} from '../toast.service';
import {ReportService} from '../services/report.service';
import {ReportDefinitionService} from '../services/report-definition.service';
import {Router} from '@angular/router';
import * as moment from 'moment';

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
    reportDef?: any,
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
    this.today = getFhirDate(this.criteria.periodStart);
  }

  get selectedReportTypeDisplay() {
    if (this.criteria.reportDef != undefined) {
      const found = this.measureConfigs.find(mc => mc.id === this.criteria.reportDef.id);
      return found ? found.name : 'Select';
    } else {
      return 'Select';
    }
  }

  async reload() {
    try {
      this.loading = true;
      this.generateReportButtonText = 'Loading...';

      if (!this.criteria.periodStart) {
        this.criteria.periodStart = getFhirNow();
      } else {
        this.criteria.periodStart = getFhirDate(this.criteria.periodStart);
      }
      const identifier = this.criteria.reportDef.system + '|' + this.criteria.reportDef.value;
      // TODO: calculate periodEnd
      const periodStart = this.criteria.periodStart;
      const periodEndDate = moment.utc(periodStart);
      periodEndDate.add(23, 'hours');
      periodEndDate.add(59, 'minutes');
      periodEndDate.add(59, 'seconds');

      try {

        const generateResponse = await this.reportService.generate(identifier, formatDateToISO(periodStart), formatDateToISO(periodEndDate));
        this.router.navigate(['review', generateResponse.reportId]);
      } catch (ex) {
        if (ex.status === 409) {
          if (confirm(ex.error.message)) {
            try {
              const identifier = this.criteria.reportDef.system + '|' + this.criteria.reportDef.value;
              const generateResponse = await this.reportService.generate(identifier, formatDateToISO(periodStart), formatDateToISO(periodEndDate), true);
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
    console.log(this.measureConfigs);
  }

  disableGenerateReport() {
    return !this.criteria.periodStart || !this.criteria.reportDef;
  }
}
