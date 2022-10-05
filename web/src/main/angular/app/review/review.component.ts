import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {AuthService} from "../services/auth.service";
import {ReportService} from "../services/report.service";
import {StoredReportDefinition} from "../model/stored-report-definition";
import {Router} from '@angular/router';
import {ReportDefinitionService} from '../services/report-definition.service';
import {formatDateToISO, getEndOfDayDate, getFhirDate} from '../helper';
import {UserModel} from "../model/user-model";
import {ToastService} from "../toast.service";

@Component({
  selector: 'nandina-review',
  templateUrl: './review.component.html',
  styleUrls: ['./review.component.css']
})
export class ReviewComponent implements OnInit {

  reports: any[];
  measures: StoredReportDefinition[] = [];
  statuses = [{name: 'In Review', value: "preliminary"}, {name: 'Submitted', value: "final"}];
  submitters: UserModel[] = [];
  page = 1;
  pageSize = 20;
  totalSize;
  loading = false;

  filter = {
    measure: 'Select measure',
    status: 'Select status',
    period: {startDate: null, endDate: null},
    submittedDate: null,
    submitter: 'Select submitter',
    reportTypeId: ''
  };

  @Output() reset: EventEmitter<any> = new EventEmitter<any>();

  constructor(public authService: AuthService,
              public reportService: ReportService,
              private reportDefinitionService: ReportDefinitionService,
              public toastService: ToastService,
              private router: Router) {
  }

  async onChangeFilters() {
    this.filter.reportTypeId = "";
    await this.searchReports();
  }

  resetFilters(reportingDate, submittedDate) {
    this.filter.reportTypeId = "";
    this.page = 1;
    this.filter.measure = 'Select measure';
    this.filter.status = 'Select status';
    this.filter.submitter = 'Select submitter';
    this.filter.submittedDate = null;
    this.filter.period = {startDate: null, endDate: null};
    reportingDate.resetDates();
    submittedDate.resetDate();
    this.searchReports();
  }

  selectPeriod(period) {
    this.filter.period.startDate = period.startDate;
    this.filter.period.endDate = period.endDate;
    this.onChangeFilters().then(() => this.page = 1).catch(error => console.log(error))

  }

  selectMeasure(measure) {
    this.filter.measure = measure;
    this.onChangeFilters().then(() => {
      this.page = 1;
    });
  }

  selectStatus(status) {
    this.filter.status = status;
    this.onChangeFilters().then(() => this.page = 1).catch(error => console.log(error))
  }

  selectSubmittedDate(submittedDate) {
    this.filter.submittedDate = submittedDate;
    this.onChangeFilters().then(() => this.page = 1).catch(error => console.log(error))
  }

  selectSubmitter(submitter) {
    this.filter.submitter = submitter;
    this.onChangeFilters().then(() => this.page = 1).catch(error => console.log(error))
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
    if (this.filter.reportTypeId == "") {
      if (this.filter.measure !== "Select measure") {
        // find the measure
        const measure = this.measures.find(p => p.name === this.filter.measure);
        filterCriteria += `identifier=${encodeURIComponent(measure[0].system + "|" + measure[0].value)}&`
      }
      if (this.filter.status !== 'Select status') {
        const status = this.statuses.find(p => p.name === this.filter.status);
        filterCriteria += `docStatus=${encodeURIComponent(status.value)}&`
      }
      if (this.filter.period.startDate !== null) {
        let startDate = formatDateToISO(getFhirDate(this.filter.period.startDate));
        filterCriteria += `periodStartDate=${startDate}&`
      }
      if (this.filter.period.endDate !== null) {
        let endDate = formatDateToISO(getFhirDate(this.filter.period.endDate));
        filterCriteria += `periodEndDate=${getEndOfDayDate(endDate)}&`
      }
      if (this.filter.submittedDate !== null) {
        let submittedDate = formatDateToISO(getFhirDate(this.filter.submittedDate));
        filterCriteria += `submittedDate=${submittedDate}&`
      }
      if (this.filter.submitter !== "Select submitter") {
        // find the submitter
        const submitter = this.submitters.find(p => p.name === this.filter.submitter);
        filterCriteria += `author=${submitter.id}`
      }
    } else {
      filterCriteria += `bundleId=${this.filter.reportTypeId}&`
      filterCriteria += `page=${this.page}`
    }
    return filterCriteria;
  }

  async searchReports() {
    const data = await this.reportService.getReports(this.getFilterCriteria());
    const reportBundle = await data;
    this.reports = reportBundle.list;
    this.totalSize = reportBundle.totalSize;
    this.filter.reportTypeId = reportBundle.reportTypeId;
  }

  getMeasureName(measure: string) {
    let measureSystem = '';
    let measureId = '';
    if (measure != null) {
      measureSystem = measure.substr(0, measure.indexOf("|"));
      measureId = measure.substr(measure.indexOf("|") + 1);
    }
    let foundMeasure = (this.measures || []).find((m) => m[0].value === measureId && m[0].system === measureSystem);
    if (foundMeasure != undefined && foundMeasure[0].value != undefined) return foundMeasure.name;
  }

  getStatusName(status: string) {
    let foundStatus;
    if (status != null) {
      foundStatus = (this.statuses || []).find((m) => m.value === status);
    }
    if (foundStatus != undefined) return foundStatus.name;
    return "";
  }

  getSubmitterName(submitterId: string) {
    let foundSubmitter = (this.submitters || []).find((m) => m.id === submitterId);
    if (foundSubmitter != undefined && foundSubmitter.id != undefined) return foundSubmitter.name;
  }

  getNote(note: string) {
    if (note && note.length > 50) {
      return note.substr(0, 50) + "...";
    }
    return note;
  }

  displayReport(reportId) {
    this.router.navigate(['review', reportId]);
  }

  async onPageChange(newPage) {
    if (newPage == 1) {
      this.filter.reportTypeId = '';
    }
    this.page = newPage;
    await this.searchReports();
  }

  async ngOnInit() {
    this.loading = true;
    try{
      await this.searchReports();
      this.measures = await this.reportDefinitionService.getReportDefinitions();
      this.submitters = await this.reportService.getSubmitters();
    }
    catch (ex){
      this.toastService.showException('Error populating report list.', ex);
    }
    finally {
      this.loading = false;
    }
  }

}
