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

  constructor(
      private route: ActivatedRoute,
      public reportService: ReportService,
      public toastService: ToastService,
      private modal: NgbModal,
      private router: Router) {
  }

  get isSubmitted() {
    if (!this.report) return false;
    return this.report.status === 'FINAL';
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
        await this.reportService.send(this.reportId);
        this.toastService.showInfo('Report sent!');
        await this.initReport();
      }
    } catch (ex) {
      this.toastService.showException('Error sending report: ' + this.reportId, ex);
    }
  }

  async download() {
    try {
      await this.reportService.download(this.reportId);
      this.toastService.showInfo('Report downloaded!');
    } catch (ex) {
      this.toastService.showException('Error downloading report: ' + this.reportId, ex);
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
    reportSaveModel.questionnaireResponse = null;
    try {
      await this.reportService.save(reportSaveModel, this.reportId);
      this.toastService.showInfo('Report saved!');
      this.dirty = false;
    } catch (ex) {
      this.toastService.showException('Error saving report: ' + this.reportId, ex);
    }
  }

  isMeasureIdentifier(measureId) {
    if (this.report != undefined && this.report.measure != undefined && this.report.measure.identifier[0].value == measureId) {
      return true;
    }
    return false;
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
