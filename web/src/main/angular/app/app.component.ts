import {Component, OnInit} from '@angular/core';
import {HttpClient, HttpResponse} from '@angular/common/http';
import {QuestionnaireResponseSimple} from './model/questionnaire-response-simple';
import {IQuestionnaireResponse, IQuestionnaireResponseItemComponent} from './fhir';
import saveAs from 'save-as';
import {LocationResponse} from './model/location-response';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {SelectLocationsComponent} from './select-locations/select-locations.component';
import {CookieService} from 'ngx-cookie-service';
import {getFhirNow} from './helper';
import {OAuthService} from 'angular-oauth2-oidc';
import {ToastService} from './toast.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  loading = false;
  response: QuestionnaireResponseSimple = new QuestionnaireResponseSimple();
  overflowLocations: LocationResponse[] = [];
  user: any;

  constructor(
      private http: HttpClient,
      private modal: NgbModal,
      private cookieService: CookieService,
      private oauthService: OAuthService,
      public toastService: ToastService) {
    if (this.cookieService.get('overflowLocations')) {
      try {
        this.overflowLocations = JSON.parse(this.cookieService.get('overflowLocations'));
      } catch (ex) {}
    }
  }

  get overflowLocationsDisplay() {
    const displays = this.overflowLocations.map(l => {
      const r = l.display || l.id;
      return r ? r.replace(/,/g, '') : 'Unknown';
    });

    return displays.join(', ');
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
    const modalRef = this.modal.open(SelectLocationsComponent, { size: 'lg' });
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
        url += 'reportDate=' + encodeURIComponent(this.response.date) + '&';
      } else {
        url += 'reportDate=' + encodeURIComponent(getFhirNow()) + '&';
      }

      this.response = await this.http.get<QuestionnaireResponseSimple>(url).toPromise();
      const keys = Object.keys(this.response);
      for (const key of keys) {
        if (this.response[key] === null) {
          delete this.response[key];
        }
      }

      this.toastService.showInfo('Successfully ran queries!');
    } catch (ex) {
      this.toastService.showException('Error running queries', ex);
    } finally {
      this.loading = false;
    }
  }

  login() {
    this.oauthService.initImplicitFlow();
  }

  logout() {
    this.oauthService.logOut();
  }

  async ngOnInit() {
    const issuer = '%auth.issuer%';
    const clientId = '%auth.clientId%';
    const scope = '%auth.scope%';

    this.oauthService.configure({
      issuer,
      redirectUri: window.location.origin + '/',
      clientId,
      responseType: 'code',
      scope,
      showDebugInformation: false,
      requestAccessToken: true,
      requireHttps: false
    });
    this.oauthService.setStorage(localStorage);
    await this.oauthService.loadDiscoveryDocument();

    const loggedIn: boolean = await this.oauthService.tryLogin();

    try {
      if (loggedIn) {
        this.user = await this.oauthService.loadUserProfile() as any;
      }
    } catch (ex) {

    }

    if (!this.user) {
      this.oauthService.initImplicitFlow();
    }
  }
}
