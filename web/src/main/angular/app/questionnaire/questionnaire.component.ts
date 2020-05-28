import {Component, OnInit} from '@angular/core';
import {QuestionnaireResponseSimple} from '../model/questionnaire-response-simple';
import {LocationResponse} from '../model/location-response';
import {formatDate, getFhirNow} from '../helper';
import {HttpClient, HttpResponse} from '@angular/common/http';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {CookieService} from 'ngx-cookie-service';
import {OAuthService} from 'angular-oauth2-oidc';
import {ToastService} from '../toast.service';
import {SelectLocationsComponent} from '../select-locations/select-locations.component';
import saveAs from 'save-as';
import {AuthService} from '../auth.service';

@Component({
  selector: 'app-questionnaire',
  templateUrl: './questionnaire.component.html',
  styleUrls: ['./questionnaire.component.css']
})
export class QuestionnaireComponent implements OnInit {
  loading = false;
  response: QuestionnaireResponseSimple = new QuestionnaireResponseSimple();
  overflowLocations: LocationResponse[] = [];
  rememberFields = '%remember.fields%';
  user: any;
  today = getFhirNow();

  constructor(
      private http: HttpClient,
      private modal: NgbModal,
      private cookieService: CookieService,
      public authService: AuthService,
      public toastService: ToastService) {
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
    this.today = formatDate(this.response.date);
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

    // loop through the remembered fields from application.properties file and save the input values in cookies
    const rememberFieldsArray = this.rememberFields.split(',');
    rememberFieldsArray.forEach(field => {
      if (this.response[field]) {
        this.cookieService.set(field, this.response[field]);
      } else if (this.cookieService.get(field)) {
        this.cookieService.delete(field);
      }
    });

    try {
      this.response.date = formatDate(this.response.date);
      convertResponse = await this.http.post('/api/convert', this.response, { observe: 'response', responseType: 'text' }).toPromise();
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

      let url = '/api/query?';

      if (this.overflowLocations.length > 0) {
        const ids = this.overflowLocations.map(ol => ol.id);
        url += 'overflowLocations=' + encodeURIComponent(ids.join(',')) + '&';
      }

      if (this.response.date) {
        url += 'reportDate=' + encodeURIComponent(formatDate(this.response.date)) + '&';
      } else {
        url += 'reportDate=' + encodeURIComponent(getFhirNow()) + '&';
      }


      try {
        this.response = await this.http.get<QuestionnaireResponseSimple>(url).toPromise();
      } catch (ex) {
        this.toastService.showException('Error converting report', ex);
        return;
      }

      const keys = Object.keys(this.response);
      for (const key of keys) {
        if (this.response[key] === null) {
          delete this.response[key];
        }
      }

      // loop through each of the remembered fields and set the value based on the value that is in the cookie
      const rememberFieldsArray = this.rememberFields.split(',');
      rememberFieldsArray.forEach(field => {
        if (!this.response[field]) {
          this.response[field] = this.cookieService.get(field);
        }
      });

      this.toastService.showInfo('Successfully ran queries!');
    } catch (ex) {
      this.toastService.showException('Error running queries', ex);
    } finally {
      this.loading = false;
    }
  }

  async login() {
    await this.authService.loginLocal();
  }

  logout() {
    this.authService.logout();
  }

  async ngOnInit() {
    // initialize the response date to today by default.
    this.response.date = getFhirNow();
  }
}
