import {Component, OnInit} from '@angular/core';
import {formatDateToISO, getFhirDate, getFhirYesterday} from '../helper';
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
  loadingMeasures = false;
  reportGenerated = false;
  startDate = getFhirYesterday();
  endDate = getFhirYesterday();
  criteria: {
    reportDef?: any,
    periodStart?: string,
    periodEnd?: string
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

  get selectedReportTypeDisplay() {
    if (this.criteria.reportDef != undefined) {
      const found = this.measureConfigs.find(mc => mc.id === this.criteria.reportDef.id);
      return found ? found.name : 'Select';
    } else {
      return 'Select';
    }
  }

  onStartDateSelected() {
    this.startDate = getFhirDate(this.criteria.periodStart);
    this.criteria.periodEnd = this.criteria.periodStart;
    this.endDate = getFhirDate(this.criteria.periodEnd)
  }

  onEndDateSelected() {
    this.endDate = getFhirDate(this.criteria.periodEnd);
  }


  async reload() {
    try {

      if (this.endDate < this.startDate) {
        alert("Report End Date must be same or after Report Start Date.");
        return;
      }
      this.loading = true;
      this.generateReportButtonText = 'Loading...';


      if (!this.criteria.periodStart) {
        this.criteria.periodStart = getFhirYesterday();
      } else {
        this.criteria.periodStart = getFhirDate(this.criteria.periodStart);
      }

      if (!this.criteria.periodEnd) {
        this.criteria.periodEnd = getFhirYesterday();
      } else {
        this.criteria.periodEnd = getFhirDate(this.criteria.periodEnd);
      }

      const identifier = this.criteria.reportDef.system + '|' + this.criteria.reportDef.value;
      const periodStart = this.criteria.periodStart;
      // add time to periodEnd
      const periodEnd = this.criteria.periodEnd;
      const periodEndDate = moment.utc(periodEnd);
      periodEndDate.add(23, 'hours');
      periodEndDate.add(59, 'minutes');
      periodEndDate.add(59, 'seconds');

      try {

        const generateResponse = await this.reportService.generate(identifier, formatDateToISO(periodStart), formatDateToISO(periodEndDate));
        await this.router.navigate(['review', generateResponse.reportId]);
      } catch (ex) {
        if (ex.status === 409) {
          if (confirm(ex.error.message)) {
            try {
              const identifier = this.criteria.reportDef.system + '|' + this.criteria.reportDef.value;
              const generateResponse = await this.reportService.generate(identifier, formatDateToISO(periodStart), formatDateToISO(periodEndDate), true);
              await this.router.navigate(['review', generateResponse.reportId]);
            } catch (ex) {
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
    try {
      this.loadingMeasures = true;
      // initialize the response date to today by default.
      this.criteria.periodStart = getFhirYesterday();
      this.criteria.periodEnd = getFhirYesterday();
      this.measureConfigs = await this.reportDefinitionService.getReportDefinitions();
      console.log(this.measureConfigs);
    } catch (ex) {
      this.toastService.showException('Error populating measure list.', ex);
    } finally {
      this.loadingMeasures = false;
    }
  }

  disableGenerateReport() {
    return !this.criteria.periodStart || !this.criteria.periodEnd || !this.criteria.reportDef;
  }
}
