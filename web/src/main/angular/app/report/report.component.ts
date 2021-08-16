import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Params} from "@angular/router";
import {Subscription} from "rxjs";
import {ReportService} from "../services/report.service";
import {ToastService} from "../toast.service";
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {ViewLineLevelComponent} from '../view-line-level/view-line-level.component';

@Component({
    selector: 'report',
    templateUrl: './report.component.html',
    styleUrls: ['./report.component.css']
})
export class ReportComponent implements OnInit, OnDestroy {
    report: { id: string };
    paramsSubscription: Subscription;

    constructor(
        private route: ActivatedRoute,
        public reportService: ReportService,
        public toastService: ToastService,
        private modal: NgbModal) {
    }

    viewLineLevel() {
        const modalRef = this.modal.open(ViewLineLevelComponent, { size: 'xl' });
        modalRef.componentInstance.reportId = this.report.id;
    }

    async send() {
        try {
            await this.reportService.send(this.report.id);
            this.toastService.showInfo('Report sent!');
        } catch (ex) {
            this.toastService.showException('Error sending report: ' + this.report.id, ex);
        }
    }

    async download() {
        try {
            await this.reportService.download(this.report.id);
            this.toastService.showInfo('Report downloaded!');
        } catch (ex) {
            this.toastService.showException('Error downloading report: ' + this.report.id, ex);
        }
    }

    ngOnInit() {
        this.report = {
            id: this.route.snapshot.params['id']
        };

        this.paramsSubscription = this.route.params.subscribe(
            (params: Params) => {
                this.report.id = params['id'];
            }
        );
    }

    ngOnDestroy() {
        this.paramsSubscription.unsubscribe();
    }
}
