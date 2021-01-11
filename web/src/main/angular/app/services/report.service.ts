import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {ConfigService} from './config.service';
import {LocationResponse} from '../model/location-response';
import {QueryReport} from '../model/query-report';
import saveAs from 'save-as';
import {MeasureConfig} from '../model/MeasureConfig';

@Injectable()
export class ReportService {
  constructor(private http: HttpClient, private configService: ConfigService) {
  }

  private getFileName(contentDisposition: string) {
    if (!contentDisposition) return 'report.txt';
    const parts = contentDisposition.split(';');
    if (parts.length !== 2 || parts[0] !== 'attachment') return 'report.txt';
    if (parts[1].indexOf('filename=') < 0) return 'report.txt';
    return parts[1].substring('filename='.length + 1).replace(/"/g, '');
  }

  async download(report: QueryReport) {
    const url = this.configService.getApiUrl('/api/download');
    const convertResponse = await this.http.post(url, report, {observe: 'response', responseType: 'blob'}).toPromise();

    saveAs(convertResponse.body, this.getFileName(convertResponse.headers.get('Content-Disposition')));
  }

  async send(report: QueryReport) {
    let url = '/api/send';

    url = this.configService.getApiUrl(url);

    return await this.http.post<QueryReport>(url, report).toPromise();
  }

  async generate(report: QueryReport, overflowLocations?: LocationResponse[]) {
    let url = '/api/query?';

    if (overflowLocations.length > 0) {
      const ids = overflowLocations.map(ol => ol.id);
      url += 'overflowLocations=' + encodeURIComponent(ids.join(',')) + '&';
    }

    url = this.configService.getApiUrl(url);

    return await this.http.post<QueryReport>(url, report).toPromise();
  }

  getMeasures() {
    const url = this.configService.getApiUrl('/api/report/measures');
    return this.http.get<MeasureConfig[]>(url).toPromise();
  }
}