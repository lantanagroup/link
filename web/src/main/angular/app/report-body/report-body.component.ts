import {Component, ComponentFactory, ComponentFactoryResolver, ComponentRef, OnInit, ViewChild} from '@angular/core';
import {LocationResponse} from '../model/location-response';
import {formatDate, getFhirNow} from '../helper';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {CookieService} from 'ngx-cookie-service';
import {ToastService} from '../toast.service';
import {SelectLocationsComponent} from '../select-locations/select-locations.component';
import {ReportService} from '../services/report.service';
import {QueryReport} from '../model/query-report';
import {ReportBodyDirective} from '../report-body.directive';
import {ConfigService} from '../services/config.service';
import {PihcReportComponent} from './pihc-report/pihc-report.component';
import {PillboxReportComponent} from './pillbox-report/pillbox-report.component';
import {IReportPlugin} from './report-plugin';
import {MeasureEvalComponent} from "./measure-eval/measure-eval.component";
import {HttpClient} from "@angular/common/http";
import {MeasureConfig} from "../model/MeasureConfig";

@Component({
  selector: 'app-report-body',
  templateUrl: './report-body.component.html',
  styleUrls: ['./report-body.component.css']
})
export class ReportBodyComponent implements OnInit {
  @ViewChild(ReportBodyDirective, {static: true}) reportBody: ReportBodyDirective;

  loading = false;
  sending = false;
  reportGenerated = false;
  overflowLocations: LocationResponse[] = [];
  today = getFhirNow();
  report: QueryReport = new QueryReport();
  measureConfigs: MeasureConfig[] = [];
  evaluateMeasureButtonText: String = 'Select Measure';
  generateReportButtonText: String = 'Generate Report';

  private reportBodyComponent: ComponentRef<IReportPlugin>;

  constructor(
      private http: HttpClient,
      private modal: NgbModal,
      private configService: ConfigService,
      private cookieService: CookieService,
      private componentFactoryResolver: ComponentFactoryResolver,
      public toastService: ToastService,
      public reportService: ReportService) {

    if (this.cookieService.get('overflowLocations')) {
      try {
        this.overflowLocations = JSON.parse(this.cookieService.get('overflowLocations'));
      } catch (ex) {
      }
    }
  }

  get overflowLocationsDisplay() {
    const displays = this.overflowLocations.map(l => {
      const r = l.display || l.id;
      return r ? r.replace(/,/g, '') : 'Unknown';
    });

    return displays.join(', ');
  }

  onDateSelected() {
    this.today = formatDate(this.report.date);
  }

  async download() {
    try {
      this.report.date = formatDate(this.report.date);
      await this.reportService.download(this.report);
    } catch (ex) {
      this.toastService.showException('Error converting report', ex);
      return;
    }
  }

  async selectOverflowLocations() {
    const selected = this.overflowLocations ? JSON.parse(JSON.stringify(this.overflowLocations)) : [];
    const modalRef = this.modal.open(SelectLocationsComponent, { size: 'lg' });
    modalRef.componentInstance.selected = selected;
    this.overflowLocations = (await modalRef.result) || [];
    this.cookieService.set('overflowLocations', JSON.stringify(this.overflowLocations));
  }

  async send() {
    try {
      this.sending = true;
      if (this.report) {
        await this.reportService.send(this.report);
      } else {
        this.toastService.showInfo('Unable to send blank report');
      }
      this.toastService.showInfo('Successfully sent report!');
    } catch (ex) {
      this.toastService.showException('Error sending report', ex);
    } finally {
      this.sending = false;
    }
  }

  async reload() {
    try {
      this.loading = true;
      this.generateReportButtonText = 'Loading...';

      if (this.evaluateMeasureButtonText) {
        this.measureConfigs.forEach(measure => {
          if (measure.name === this.evaluateMeasureButtonText) {
            this.report.measureId = measure.id;
          }
        })
      }

      if (!this.report.date) {
        this.report.date = getFhirNow();
      } else {
        this.report.date = formatDate(this.report.date);
      }

      try {
        const updatedReport = await this.reportService.generate(this.report, this.overflowLocations);
        Object.assign(this.report, updatedReport);
      } catch (ex) {
        this.toastService.showException('Error converting report', ex);
        return;
      }

      const keys = Object.keys(this.report);
      for (const key of keys) {
        if (this.report[key] === null) {
          delete this.report[key];
        }
      }

      if (this.reportBodyComponent) {
        this.reportBodyComponent.instance.refreshed();
      }

      this.toastService.showInfo('Successfully ran queries!');
      this.reportGenerated = true;
    } catch (ex) {
      this.toastService.showException('Error running queries', ex);
    } finally {
      this.loading = false;
      this.generateReportButtonText = 'Generate Report';
    }
  }

  loadReportBody() {
    let componentFactory: ComponentFactory<IReportPlugin>;

    switch (this.configService.config.report as any) {
      case 'pihc':
        componentFactory = this.componentFactoryResolver.resolveComponentFactory(PihcReportComponent);
        break;
      case 'pillbox':
        componentFactory = this.componentFactoryResolver.resolveComponentFactory(PillboxReportComponent);
        break;
      case 'measure-eval':
        componentFactory = this.componentFactoryResolver.resolveComponentFactory(MeasureEvalComponent);
        break;
    }

    const viewContainerRef = this.reportBody.viewContainerRef;
    viewContainerRef.clear();

    this.reportBodyComponent = viewContainerRef.createComponent(componentFactory);
    this.reportBodyComponent.instance.report = this.report;
  }

  selectMeasure(selectedItem: string){
    this.evaluateMeasureButtonText = selectedItem;
  }

  async ngOnInit() {
    // initialize the response date to today by default.
    this.report.date = getFhirNow();

    this.measureConfigs = await this.reportService.getMeasures();

    this.loadReportBody();
  }

  disableGenerateReport() {
    if (this.evaluateMeasureButtonText === 'Select Measure') {
      return true;
    } else {
      return false;
    }
  }
}
