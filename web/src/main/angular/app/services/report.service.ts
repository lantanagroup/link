import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {ConfigService} from './config.service';
import {QueryReport} from '../model/query-report';
import saveAs from 'save-as';
import {ReportBundle} from '../model/ReportBundle';
import {UserModel} from "../model/UserModel";
import {GenerateResponse} from '../model/generate-response';

@Injectable()
export class ReportService {
  constructor(private http: HttpClient, private configService: ConfigService) {
  }

  private getFileName(contentDisposition: string, contentType: string) {
    if (!contentDisposition && contentType === 'application/xml') return 'report.xml';
    else if (!contentDisposition && contentType === 'application/json') return 'report.json';
    else if (!contentDisposition) return 'report.txt';

    const parts = contentDisposition.split(';');
    if (parts.length !== 2 || parts[0] !== 'attachment') return 'report.txt';
    if (parts[1].indexOf('filename=') < 0) return 'report.txt';
    return parts[1].substring('filename='.length + 1).replace(/"/g, '');
  }

  async generate(reportDefId: string, periodStart: string, periodEnd: string, regenerate = false) {
    let url = '/api/report/$generate?';
    url += `reportDefId=${encodeURIComponent(reportDefId)}&`;
    url += `periodStart=${encodeURIComponent(periodStart)}&`;
    url += `periodEnd=${encodeURIComponent(periodEnd)}&`;
    url += 'regenerate=' + (regenerate ? 'true' : 'false' );
    url = this.configService.getApiUrl(url);

    return await this.http.post<GenerateResponse>(url, null).toPromise();
  }

  getReports(queryParams) {
    let url = this.configService.getApiUrl('/api/report/searchReports?');
    if (queryParams != undefined && queryParams != ""){
      url += queryParams;
    }
    return this.http.get<ReportBundle>(url).toPromise();
  }

  getSubmitters() {
    const url = this.configService.getApiUrl('/api/user');
    return this.http.get<UserModel[]>(url).toPromise();
  }

  async send(reportId: string) {
    const url = this.configService.getApiUrl(`/api/report/${encodeURIComponent(reportId)}/$send`);
    return this.http.get(url).toPromise();
  }

  async download(reportId: string) {
    const url = this.configService.getApiUrl(`/api/report/${encodeURIComponent(reportId)}/$download`);
    const downloadResponse = await this.http.get(url, {observe: 'response', responseType: 'blob'}).toPromise();

    const contentDisposition = downloadResponse.headers.get('Content-Disposition');
    const contentType = downloadResponse.headers.get('Content-Type');

    saveAs(downloadResponse.body, this.getFileName(contentDisposition, contentType));
  }
}
