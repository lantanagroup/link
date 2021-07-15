import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Params} from "@angular/router";
import {Subscription} from "rxjs";
import {ReportService} from "../services/report.service";
import {ToastService} from "../toast.service";

@Component({
    selector: 'report',
    templateUrl: './report.component.html',
    styleUrls: ['./report.component.css']
})
export class ReportComponent implements OnInit, OnDestroy {
    report: { id: number };
    paramsSubscription: Subscription;
    submitReportButtonText: String = 'Submit';

    constructor(private route: ActivatedRoute, public reportService: ReportService, public toastService: ToastService,) {
    }

    async sendReport() {
        try {
            await this.reportService.sendReport(this.report.id);
        } catch (ex) {
            this.toastService.showException('Error sending report: ' + this.report.id, ex);
            return;
        }
        this.toastService.showInfo('Report sent!');
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
