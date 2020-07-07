import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {ConfigService} from './config.service';
import {formatDate, getFhirNow} from '../helper';
import {LocationResponse} from '../model/location-response';
import {QueryReport} from '../model/query-report';

@Injectable()
export class ReportService {
  constructor(private http: HttpClient, private configService: ConfigService) {
  }

  async convert(report: QueryReport) {
    const url = this.configService.getApiUrl('/api/convert');
    return await this.http.post(url, report, { observe: 'response', responseType: 'text' }).toPromise();
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
}