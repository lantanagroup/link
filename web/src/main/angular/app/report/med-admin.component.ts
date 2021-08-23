import {Component, Input, OnDestroy, OnInit} from '@angular/core';

@Component({
    selector: 'app-report-med-admin',
    templateUrl: './med-admin.component.html',
    styleUrls: ['./med-admin.component.css']
})
export class MedAdminComponent implements OnInit, OnDestroy {

    @Input() report: any;


    ngOnInit() {

    }

    ngOnDestroy() {

    }
}
