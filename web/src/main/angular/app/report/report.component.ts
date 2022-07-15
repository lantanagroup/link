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
  reportId: string;
  report: ReportModel;
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

  get repVersionNumber() {
    return Number(this.report.version);
  }

  get isSubmitted() {
    if (!this.report) return false;
    return this.report.status === 'FINAL';
  }

  get measureId() {
    if (this.report && this.report.measure && this.report.measure.identifier && this.report.measure.identifier.length > 0) {
      return this.report.measure.identifier[0].value;
    }
  }

  setRequiredErrorsFlag(hasErrors) {
    if ('boolean' === typeof hasErrors) {
      this.hasRequiredErrors = hasErrors;
    }
  }

  setDirtyFlag(dirty) {
    this.dirty = dirty;
  }

  viewLineLevel() {
    const modalRef = this.modal.open(ViewLineLevelComponent, {size: 'xl'});
    modalRef.componentInstance.reportId = this.reportId;
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

  get submitText(){
    if(this.submitInProgress){
      return "Submitting...";
    }
    else return "Submit";
  }

  async send() {
    try {
      if (this.dirty) {
        if (confirm('Before submitting, do you want to save your changes?')) {
          await this.save();
        } else {
          return;
        }
      }
      if (confirm('Are you sure you want to submit this report?')) {
        this.submitInProgress = true;
        await this.reportService.send(this.reportId);
        this.toastService.showInfo('Report sent!');
        await this.router.navigate(['/review']);
      }
    } catch (ex) {
      this.toastService.showException('Error sending report: ' + this.reportId, ex);
    } finally {
      this.submitInProgress = false;
    }
  }

  async download() {
    try {
      this.downloading = true;
      await this.reportService.download(this.reportId);
      this.toastService.showInfo('Report downloaded!');
    } catch (ex) {
      this.toastService.showException('Error downloading report: ' + this.reportId, ex);
    } finally {
      this.downloading = false;
    }
  }

  async discard() {
    try {
      if (confirm('Are you sure you want to discard this report?')) {
        await this.reportService.discard(this.reportId);
        await this.router.navigate(['/review']);
      }
    } catch (ex) {
      this.toastService.showException('Error discarding report: ' + this.reportId, ex);
    }
  }

  async initReport() {
    this.loading = true;

    try {
      this.report = await this.reportService.getReport(this.reportId);
    } catch (ex) {
      this.toastService.showException('Error loading report', ex);
    } finally {
      this.loading = false;
    }
  }

  getStatusDisplay() {
    switch (this.report.status.toLowerCase()) {
      case 'preliminary':
        return 'Reviewing';
      case 'final':
        return 'Submitted';
    }
  }

  async save() {
    let reportSaveModel = new ReportSaveModel();
    reportSaveModel.measureReport = this.report.measureReport;
    try {
      await this.reportService.save(reportSaveModel, this.reportId);
      this.toastService.showInfo('Report saved!');
      this.dirty = false;
    } catch (ex) {
      this.toastService.showException('Error saving report: ' + this.reportId, ex);
    }
  }

  async regenerate() {
    this.loading = true;
    this.regenerateReportButtonText = 'Loading...';
    const identifier = this.report.measure.identifier[0].system + " | " + this.report.measure.identifier[0].value;
    try {
      if (confirm('Confirm re-generate?\n Re-generating will re-acquire the latest date from the EHR for the period and re-evaluate the measure. ' +
          'Once re-evaluated, the newest calculated aggregate totals will be updated in this report. ' +
          'Fields not calculated as part of the measure will not be affected. Are you sure you want to continue?')) {
        try {
          const generateResponse = await this.reportService.generate(identifier, formatDateToISO(this.report.measureReport.period.start), formatDateToISO(this.report.measureReport.period.end), true);
          await this.router.navigate(['review', generateResponse.reportId]);
          this.toastService.showInfo('Report re-generated!');
          await this.initReport();
        } catch (ex) {
          if (ex.status === 409) {
            if (confirm(ex.error.message)) {
              try {
                const generateResponse = await this.reportService.generate(identifier, formatDateToISO(this.report.measureReport.period.start), formatDateToISO(this.report.measureReport.period.end), true);
                await this.router.navigate(['review', generateResponse.reportId]);
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
      this.toastService.showException('Error re-generating report: ' + this.reportId, ex);
    } finally {
      this.loading = false;
      this.regenerateReportButtonText = 'Re-generate';
    }
  }

  async ngOnInit() {
    this.reportId = this.route.snapshot.params['id']
    await this.initReport();

    this.paramsSubscription = this.route.params.subscribe(
        (params: Params) => {
          this.reportId = params['id'];
        }
    );
  }

  ngOnDestroy() {
    this.paramsSubscription.unsubscribe();
  }
}
