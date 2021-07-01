import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {AuthService} from "../services/auth.service";
import {ReportService} from "../services/report.service";
import {StoredReportDefinition} from "../model/stored-report-definition";
import {NgbDateParserFormatter, NgbDateStruct} from "@ng-bootstrap/ng-bootstrap";
import {Router} from '@angular/router';
import {ReportDefinitionService} from '../services/report-definition.service';

@Component({
    selector: 'nandina-review',
    templateUrl: './review.component.html',
    styleUrls: ['./review.component.css']
})
export class ReviewComponent implements OnInit {

    reports: any[];
    measures: StoredReportDefinition[] = [];
    statuses = [{name: 'In Review', value: "preliminary"}, {name: 'Submitted', value: "final"}];
    submitters = [];
    measure: String = 'Select measure';
    status: String = 'Select status';
    page = 1;
    bundleId;

    submittedDate: NgbDateStruct;
    submitter = "Select Submitter";

    @Output() change: EventEmitter<void> = new EventEmitter<void>();

    constructor(public authService: AuthService, public reportService: ReportService, private reportDefinitionService: ReportDefinitionService, private router: Router, private ngbDateParserFormatter: NgbDateParserFormatter) {
    }

    filter = {
        measure: 'Select measure',
        status: 'Select status',
        period: {startDate: undefined, endDate: undefined}
    };


    selectPeriod(period) {
        let startDate = new Date();
        startDate.setFullYear(period.startDate.year, period.startDate.month, period.startDate.day);
        this.filter.period.startDate = startDate;
        let endDate = new Date();
        startDate.setFullYear(period.endDate.year, period.endDate.month, period.endDate.day);
        this.filter.period.endDate = endDate;
        this.searchReports();
    }

    selectMeasure(measure) {
        this.filter.measure = measure;
        this.searchReports();
    }

    selectStatus(status) {
        this.filter.status = status;
        this.searchReports();
    }

    selectSubmittedDate(submittedDate) {
        this.submittedDate = submittedDate;
    }

    selectSubmitter(submitter) {
        this.submitter = submitter;
    }

    getLabel(status: string) {
        if (status == 'preliminary') {
            return "Edit";
        } else {
            return "Review";
        }
    }

    getFilterCriteria() {
        let filterCriteria = '';
        if (this.filter.measure !== "Select measure") {
            // find the measure
            const measure = this.measures.find(p => p.name === this.filter.measure);
            filterCriteria += `identifier=${encodeURIComponent(measure.system + "|" + measure.value)}&`
        }
        if (this.filter.status !== 'Select status') {
            const status = this.statuses.find(p => p.name === this.filter.status);
            filterCriteria += `docStatus=${encodeURIComponent(status.value)}&`
        }

        if (this.filter.period.startDate !== undefined) {
            //let myDate = this.filter.period.startDate.year + "-0" +  this.filter.period.startDate.month + "-0" + this.filter.period.startDate.day;
            let startDate = this.ngbDateParserFormatter.format(this.filter.period.startDate);
            filterCriteria += `periodStartDate=${startDate}&`
        }
        if (this.filter.period.endDate !== undefined) {
            // let myDate = this.filter.period.endDate.year + "-0" +  this.filter.period.endDate.month + "-0" + this.filter.period.endDate.day;
            let endDate = this.ngbDateParserFormatter.format(this.filter.period.endDate);
            filterCriteria += `periodEndDate=${endDate}`
        }
        return filterCriteria;
    }

    public async searchReports() {
        const data = await this.reportService.getReports(this.getFilterCriteria());
        const reportBundle = await data;
        this.reports = reportBundle.list;
        this.bundleId = reportBundle.bundleId;
    }

    getMeasureName(measure: string) {
        let measureSystem = '';
        let measureId = '';
        if (measure != null) {
            measureSystem = measure.substr(0, measure.indexOf("|"));
            measureId = measure.substr(measure.indexOf("|") + 1);
        }
        let foundMeasure = (this.measures || []).find((m) => m.value === measureId && m.system === measureSystem);
        if (foundMeasure != undefined && foundMeasure.value != undefined) return foundMeasure.name;
    }

    getStatusName(status: string) {
        let foundStatus;
        if (status != null) {
            foundStatus = (this.statuses || []).find((m) => m.value === status);
        }
        if (foundStatus != undefined) return foundStatus.name;
        return "";
    }

    displayReport(reportId) {
        this.router.navigateByUrl("/review/" + reportId);
    }

    async ngOnInit() {
        await this.searchReports();
        this.measures = await this.reportDefinitionService.getReportDefinitions();
    }
}
