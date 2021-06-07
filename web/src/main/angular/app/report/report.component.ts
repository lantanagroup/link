import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Params} from "@angular/router";
import {Subscription} from "rxjs";

@Component({
    selector: 'nandina-report',
    templateUrl: './report.component.html',
    styleUrls: ['./report.component.css']
})
export class ReportComponent implements OnInit, OnDestroy {
    report: { id: number };

    constructor(private route: ActivatedRoute) {
    }

    paramsSubscription: Subscription;

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
