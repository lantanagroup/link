import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Params} from "@angular/router";
import {Subscription} from "rxjs";
import {ReportService} from "../services/report.service";
import {ToastService} from "../toast.service";
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {ViewLineLevelComponent} from '../view-line-level/view-line-level.component';
import {ReportModel} from "../model/ReportModel";

@Component({
    selector: 'report',
    templateUrl: './report.component.html',
    styleUrls: ['./report.component.css']
})
export class ReportComponent implements OnInit, OnDestroy {
    reportId: string;
    report: ReportModel;
    paramsSubscription: Subscription;

    constructor(
        private route: ActivatedRoute,
        public reportService: ReportService,
        public toastService: ToastService,
        private modal: NgbModal) {
    }

    viewLineLevel() {
        const modalRef = this.modal.open(ViewLineLevelComponent, { size: 'xl' });
        modalRef.componentInstance.reportId = this.reportId;
    }

    async send() {
        try {
            await this.reportService.send(this.reportId);
            this.toastService.showInfo('Report sent!');
        } catch (ex) {
            this.toastService.showException('Error sending report: ' + this.report.id, ex);
        }
    }

    async download() {
        try {
            await this.reportService.download(this.reportId);
            this.toastService.showInfo('Report downloaded!');
        } catch (ex) {
            this.toastService.showException('Error downloading report: ' + this.report.id, ex);
        }
    }

    async initReport() {
        this.report = await this.reportService.getReport(this.reportId);
        console.log("report is: " + this.report.measure.identifier[0].value);
    }

    async save() {
        try {
            await this.reportService.save(this.report);
            this.toastService.showInfo('Report saved!');
        } catch (ex) {
            this.toastService.showException('Error saving report: ' + this.reportId, ex);
        }
    }

    isMeasureIdentifier(measureId) {
        if (this.report != undefined && this.report.measure.identifier[0].value == measureId) {
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
