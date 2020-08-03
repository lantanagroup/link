import {Component, OnInit} from '@angular/core';
import {LocationResponse} from '../model/location-response';
import {formatDate, getFhirNow} from '../helper';
import {HttpResponse} from '@angular/common/http';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {CookieService} from 'ngx-cookie-service';
import {ToastService} from '../toast.service';
import {SelectLocationsComponent} from '../select-locations/select-locations.component';
import {ReportService} from '../services/report.service';
import saveAs from 'save-as';
import {QueryReport} from '../model/query-report';

@Component({
  selector: 'app-questionnaire',
  templateUrl: './questionnaire.component.html',
  styleUrls: ['./questionnaire.component.css']
})
export class QuestionnaireComponent implements OnInit {
  loading = false;
  overflowLocations: LocationResponse[] = [];
  today = getFhirNow();
  report: QueryReport = new QueryReport(
      'facilityId',
      'summaryCensusId',
      'numc19confnewadm',
      'numc19suspnewadm',
      'numc19honewpats',
      'numConfC19HONewPats',
      'numC19HospPats',
      'numConfC19HospPats',
      'numC19MechVentPats',
      'numConfC19MechVentPats',
      'numC19HOPats',
      'numConfC19HOPats',
      'numC19OverflowPats',
      'numConfC19OverflowPats',
      'numC19OFMechVentPats',
      'numConfC19OFMechVentPats',
      'numc19prevdied',
      'numConfC19PrevDied',
      'numTotBeds',
      'numBeds',
      'numBedsOcc',
      'numICUBeds',
      'numNICUBeds',
      'numICUBedsOcc',
      'mechanicalVentilators',
      'mechanicalVentilatorsUsed');

  constructor(
      private modal: NgbModal,
      private cookieService: CookieService,
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

  private getFileName(contentDisposition: string) {
    if (!contentDisposition) return 'report.txt';
    const parts = contentDisposition.split(';');
    if (parts.length !== 2 || parts[0] !== 'attachment') return 'report.txt';
    if (parts[1].indexOf('filename=') < 0) return 'report.txt';
    return parts[1].substring('filename='.length + 1).replace(/"/g, '');
  }

  async download() {
    let convertResponse: HttpResponse<string>;
    try {
      this.report.date = formatDate(this.report.date);
      convertResponse = await this.reportService.convert(this.report);
    } catch (ex) {
      this.toastService.showException('Error converting report', ex);
      return;
    }

    const contentType = convertResponse.headers.get('Content-Type');
    const blob = new Blob([convertResponse.body], {type: contentType});

    saveAs(blob, this.getFileName(convertResponse.headers.get('Content-Disposition')));
  }

  async selectOverflowLocations() {
    const selected = this.overflowLocations ? JSON.parse(JSON.stringify(this.overflowLocations)) : [];
    const modalRef = this.modal.open(SelectLocationsComponent, { size: 'lg' });
    modalRef.componentInstance.selected = selected;
    this.overflowLocations = (await modalRef.result) || [];
    this.cookieService.set('overflowLocations', JSON.stringify(this.overflowLocations));
  }

  async reload() {
    try {
      this.loading = true;

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

      this.toastService.showInfo('Successfully ran queries!');
    } catch (ex) {
      this.toastService.showException('Error running queries', ex);
    } finally {
      this.loading = false;
    }
  }

  async ngOnInit() {
    // initialize the response date to today by default.
    this.report.date = getFhirNow();
  }
}
