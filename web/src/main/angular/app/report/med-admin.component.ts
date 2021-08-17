import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Params} from "@angular/router";
import {Subscription} from "rxjs";
import {ReportService} from "../services/report.service";
import {ToastService} from "../toast.service";
import {IMeasureReport} from "../fhir";

@Component({
    selector: 'app-report-med-admin',
    templateUrl: './med-admin.component.html',
    styleUrls: ['./med-admin.component.css']
})
export class MedAdminComponent implements OnInit, OnDestroy {

    @Input() measureReport: any;


    ngOnInit() {

    }

    ngOnDestroy() {

    }
}
