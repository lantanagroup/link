import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {ConfigService} from './config.service';
import saveAs from 'save-as';
import {ReportBundle} from '../model/report-bundle';
import {UserModel} from "../model/user-model";
import {GenerateResponse} from '../model/generate-response';
import {ReportPatient} from '../model/report-patient';
import {map} from 'rxjs/operators';
import {ReportModel} from "../model/ReportModel";
import {ReportSaveModel} from "../model/ReportSaveModel";
import {PatientDataModel} from "../model/PatientDataModel";

@Injectable()
export class ReportService {
  constructor(private http: HttpClient, private configService: ConfigService) {
  }

  async generate(reportDefIdentifier: string, periodStart: string, periodEnd: string, regenerate = false) {
    let url = 'report/$generate?';
    url += `reportDefIdentifier=${encodeURIComponent(reportDefIdentifier)}&`;
    url += `periodStart=${encodeURIComponent(periodStart)}&`;
    url += `periodEnd=${encodeURIComponent(periodEnd)}&`;
    url += 'regenerate=' + (regenerate ? 'true' : 'false');
    url = this.configService.getApiUrl(url);

    return await this.http.post<GenerateResponse>(url, null).toPromise();
  }

  getReports(queryParams) {
    let url = this.configService.getApiUrl('report/searchReports?');
    if (queryParams != undefined && queryParams != "") {
      url += queryParams;
    }
    return this.http.get<ReportBundle>(url).toPromise();
  }

  getSubmitters() {
    const url = this.configService.getApiUrl('user');
    return this.http.get<UserModel[]>(url).toPromise();
  }

  async send(reportId: string) {
    const url = this.configService.getApiUrl(`report/${encodeURIComponent(reportId)}/$send`);
    return this.http.get(url).toPromise();
  }

  async download(reportId: string) {
    const url = this.configService.getApiUrl(`report/${encodeURIComponent(reportId)}/$download`);
    const downloadResponse = await this.http.get(url, {observe: 'response', responseType: 'blob'}).toPromise();
    const contentType = downloadResponse.headers.get('Content-Type');

    let fileName = `${reportId}.txt`;

    if (contentType) {
      switch (contentType.toLowerCase().trim()) {
        case 'application/xml':
          fileName = `${reportId}.xml`;
          break;
        case 'application/json':
          fileName = `${reportId}.json`;
          break;
      }
    }

    saveAs(downloadResponse.body, fileName);
  }

  async getReportPatients(reportId: string) {
    const url = this.configService.getApiUrl(`report/${encodeURIComponent(reportId)}/patient`);
    return this.http.get<ReportPatient[]>(url)
        // map the array of objects from the response to new instances of ReportPatient
        .pipe(map(response => {
          return (<any[]>response).map(item => {
            const newReportPatient = new ReportPatient();
            Object.assign(newReportPatient, item);
            return newReportPatient;
          });
        })).toPromise();
  }

  async getReport(reportId: string) {
    const url = this.configService.getApiUrl(`report/${encodeURIComponent(reportId)}`);
    return await this.http.get<ReportModel>(url).toPromise();
  }

  async save(report: ReportSaveModel, id: string) {
    const url = this.configService.getApiUrl(`report/` + id);
    return await this.http.put<ReportSaveModel>(url, report).toPromise();
  }

  async discard(reportId: string) {
    const url = this.configService.getApiUrl(`report/${encodeURIComponent(reportId)}`);
    return await this.http.delete(url).toPromise();
  }

  async getPatientData(reportId: string, patientId: string) {
    const url = this.configService.getApiUrl(`report/${encodeURIComponent(reportId)}/patient/${encodeURIComponent(patientId)}`);
    return await this.http.get<PatientDataModel>(url).toPromise();
  }


  async excludePatients(reportId: string, excludedPatients: any) {
    const url = this.configService.getApiUrl(`report/${encodeURIComponent(reportId)}/$exclude`);
    return await this.http.post<void>(url, excludedPatients).toPromise();
  }

}
