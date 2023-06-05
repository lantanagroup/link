import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Params, Router} from "@angular/router";
import {Subscription} from "rxjs";
import {ReportService} from "../services/report.service";
import {ToastService} from "../toast.service";
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {ViewLineLevelComponent} from '../view-line-level/view-line-level.component';
import {ReportModel} from "../model/ReportModel";
import {ReportSaveModel} from "../model/ReportSaveModel"
import {CodeableConcept} from "../model/fhir";
import {formatDateToISO} from "../helper";

@Component({
  selector: 'report',
  templateUrl: './report.component.html',
  styleUrls: ['./report.component.css']
})
export class ReportComponent implements OnInit, OnDestroy {
  masterId: string;
  periodStart: string;
  periodEnd: string;
  reportModel: ReportModel;
  reportMeasureList: any[];
  paramsSubscription: Subscription;
  loading = false;
  hasRequiredErrors = false;
  dirty = false;
  regenerateReportButtonText: String = 'Re-generate';
  submitInProgress = false;
  downloading = false;

  constructor(
      private route: ActivatedRoute,
      public reportService: ReportService,
      public toastService: ToastService,
      private modal: NgbModal,
      private router: Router) {
  }

  get submitText() {
    if (this.submitInProgress) {
      return "Submitting...";
    } else return "Submit Bundle";
  }

  getRepVersionNumber() {
    if (!this.reportModel) return 0;
    return Number(this.reportModel.version);
  }


  isSubmitted() {
    if (!this.reportModel) return false;
    return this.reportModel.status === 'FINAL';
  }

  setRequiredErrorsFlag(hasErrors) {
    if ('boolean' === typeof hasErrors) {
      this.hasRequiredErrors = hasErrors;
    }
  }

  setDirtyFlag(dirty) {
    this.dirty = dirty;
  }

  viewLineLevel(report) {
    const modalRef = this.modal.open(ViewLineLevelComponent, {size: 'xl'});
    modalRef.componentInstance.reportId = report.measureReport.id;
  }

  getCodeableConceptCode(codeableConcept: CodeableConcept) {
    if (codeableConcept && codeableConcept.coding && codeableConcept.coding.length > 0) {
      return codeableConcept.coding[0].code;
    }
  }

  getCodeableConceptDisplay(codeableConcept: CodeableConcept) {
    if (!codeableConcept) return '';

    if (codeableConcept.text) {
      return codeableConcept.text;
    } else if (codeableConcept.coding && codeableConcept.coding.length > 0) {
      return codeableConcept.coding[0].display;
    }
  }

  getMeasureId(report) {
    if (report && report.measure && report.measure.identifier && report.measure.identifier.length > 0) {
      return report.measure.identifier[0].value;
    }
  }

  async send() {
    try {
      if (this.dirty) {
        if (confirm('Before submitting, do you want to save your changes?')) {
          for (const measureReport of this.reportMeasureList) {
            await this.save(measureReport);
          }
        } else {
          return;
        }
      }
      if (confirm('Are you sure you want to submit this report?')) {
        this.submitInProgress = true;
        await this.reportService.send(this.masterId.split('-')[0]);
        this.toastService.showInfo('Report sent!');
        await this.router.navigate(['/review']);
      }
    } catch (ex) {
      this.toastService.showException('Error sending report: ' + this.masterId, ex);
    } finally {
      this.submitInProgress = false;
    }
  }

  async download(type: string) {
    try {
      this.downloading = true;
      await this.reportService.download(this.masterId, type);
      this.toastService.showInfo('Report downloaded!');
    } catch (ex) {
      this.toastService.showException('Error downloading report: ' + this.masterId, ex);
    } finally {
      this.downloading = false;
    }
  }

  async discard(report) {
    try {
      if (confirm('Are you sure you want to discard this report?')) {
        await this.reportService.discard(report.measureReport.id);
        await this.router.navigate(['/review']);
      }
    } catch (ex) {
      this.toastService.showException('Error discarding report: ' + report.measureReport.id, ex);
    }
  }

  async initReport() {
    this.loading = true;

    try {
      this.reportModel = await this.reportService.getReport(this.masterId.split('-')[0]);
      const reportBundle = await this.reportModel;
      this.reportMeasureList = reportBundle.reportMeasureList || [];
    } catch (ex) {
      this.toastService.showException('Error loading report', ex);
    } finally {
      this.loading = false;
    }
  }

  getStatusDisplay() {
    switch (this.reportModel && this.reportModel.status.toLowerCase()) {
      case 'preliminary':
        return 'Reviewing';
      case 'final':
        return 'Submitted';
    }
  }

  async save(report) {
    const reportSaveModel = new ReportSaveModel();
    reportSaveModel.measureReport = report;
    try {
      await this.reportService.save(reportSaveModel, report.masterReport.reportId);
      this.toastService.showInfo('Report saved!');
      this.dirty = false;
    } catch (ex) {
      this.toastService.showException('Error saving report: ' + report.masterReport.reportId, ex);
    }
  }

  async regenerate() {
    this.loading = true;
    this.regenerateReportButtonText = 'Loading...';
    const bundleIds = (this.reportMeasureList || []).map(reportMeasure => reportMeasure.bundleId) + '';

    try {
      if (confirm('Confirm re-generate?\n Re-generating will re-acquire the latest date from the EHR for the period and re-evaluate the measure. ' +
          'Once re-evaluated, the newest calculated aggregate totals will be updated in this report. ' +
          'Fields not calculated as part of the measure will not be affected. Are you sure you want to continue?')) {
        try {
          const generateResponse = await this.reportService.generate(bundleIds, formatDateToISO(this.reportMeasureList[0].measureReport.period.start), formatDateToISO(this.reportMeasureList[0].measureReport.period.end), true);
          await this.router.navigate(['review', generateResponse.masterId]);
          this.toastService.showInfo('Report re-generated!');
          await this.initReport();
        } catch (ex) {
          if (ex.status === 409) {
            if (confirm(ex.error.message)) {
              try {
                const generateResponse = await this.reportService.generate(bundleIds, formatDateToISO(this.reportMeasureList[0].measureReport.period.start), formatDateToISO(this.reportMeasureList[0].measureReport.period.end), true);
                await this.router.navigate(['review', generateResponse.masterId]);
              } catch (ex) {
                this.toastService.showException('Error re-generating report', ex);
              }
            }
          } else {
            this.toastService.showException('Error re-generating report', ex);
          }
          return;
        }
      }
    } catch (ex) {
      this.toastService.showException('Error re-generating report: ' + this.masterId, ex);
    } finally {
      this.loading = false;
      this.regenerateReportButtonText = 'Re-generate';
    }
  }

  async ngOnInit() {
    this.masterId = this.route.snapshot.params['id'];
    this.periodStart = this.route.snapshot.params['periodStart'];
    this.periodEnd = this.route.snapshot.params['periodEnd'];
    await this.initReport();

    this.paramsSubscription = this.route.params.subscribe(
        (params: Params) => {
          this.masterId = params.id;
          this.periodStart = params.periodStart;
          this.periodEnd = params.periodEnd;
        }
    );
  }

  ngOnDestroy() {
    this.paramsSubscription.unsubscribe();
  }
}
