import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {ConfigService} from './config.service';
import {QueryReport} from '../model/query-report';
import saveAs from 'save-as';
import {MeasureConfig} from '../model/MeasureConfig';
import {ReportBundle} from '../model/ReportBundle';
import {UserModel} from "../model/UserModel";

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

  async download(report: QueryReport) {
    const url = this.configService.getApiUrl('/api/report/download');
    const convertResponse = await this.http.post(url, report, {observe: 'response', responseType: 'blob'}).toPromise();

    const contentDisposition = convertResponse.headers.get('Content-Disposition');
    const contentType = convertResponse.headers.get('Content-Type');

    saveAs(convertResponse.body, this.getFileName(contentDisposition, contentType));
  }

  async send(report: QueryReport) {
    let url = '/api/report/send';
    url = this.configService.getApiUrl(url);
    return await this.http.post<QueryReport>(url, report).toPromise();
  }

  async generate(report: QueryReport, regenerate:boolean) {
    let url = '/api/report/$generate?';
    url+= 'regenerate=' + ( regenerate?'true':'false');
    url = this.configService.getApiUrl(url);
    return await this.http.post<QueryReport>(url, report).toPromise();
  }

  getMeasures() {
    const url = this.configService.getApiUrl('/api/report/measures');
    return this.http.get<MeasureConfig[]>(url).toPromise();
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

}
