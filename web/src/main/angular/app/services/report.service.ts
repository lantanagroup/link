import {Injectable} from '@angular/core';
import {QuestionnaireResponseSimple} from '../model/questionnaire-response-simple';
import {HttpClient} from '@angular/common/http';
import {ConfigService} from './config.service';
import {formatDate, getFhirNow} from '../helper';
import {LocationResponse} from '../model/location-response';

@Injectable()
export class ReportService {
  constructor(private http: HttpClient, private configService: ConfigService) {
  }

  async convert(report: QuestionnaireResponseSimple) {
    const url = this.configService.getApiUrl('/api/convert');
    return await this.http.post('/api/convert', report, { observe: 'response', responseType: 'text' }).toPromise();
  }

  async generate(overflowLocations?: LocationResponse[], reportDate?: string) {
    let url = '/api/query?';

    if (overflowLocations.length > 0) {
      const ids = overflowLocations.map(ol => ol.id);
      url += 'overflowLocations=' + encodeURIComponent(ids.join(',')) + '&';
    }

    if (reportDate) {
      url += 'reportDate=' + encodeURIComponent(formatDate(reportDate)) + '&';
    } else {
      url += 'reportDate=' + encodeURIComponent(getFhirNow()) + '&';
    }

    url = this.configService.getApiUrl(url);

    return await this.http.get<QuestionnaireResponseSimple>(url).toPromise();
  }
}