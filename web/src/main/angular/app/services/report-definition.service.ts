import {Injectable} from '@angular/core';
import {StoredReportDefinition} from '../model/stored-report-definition';
import {ConfigService} from './config.service';
import {HttpClient} from '@angular/common/http';

@Injectable()
export class ReportDefinitionService {
  constructor(private configService: ConfigService, private http: HttpClient) {
  }

  getReportDefinitions() {
    const url = this.configService.getApiUrl('measure');
    return this.http.get<StoredReportDefinition[]>(url).toPromise();
  }
}
