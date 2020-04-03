import {Component, OnInit} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import saveAs from 'save-as';
import {google} from 'google-maps';
import {ReportTableInfo} from "./report-table-info";
import {IBundle} from "./fhir";

interface ClientReportResponse {
  bundle: string;
  positions: {
    latitude: number;
    longitude: number;
  }[];
}

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  loading = false;
  error: string;
  message: string;
  loadingCount = 0;
  data: {
    entry: {
      resource: {
        entry: {
          resource: any;
        }[]
      };
    }[];
  };
  coordinates: {
    latitude: number;
    longitude: number;
  }[];
  reportTableInfo: ReportTableInfo[];

  constructor(private http: HttpClient) {
  }

  async download(format: 'json'|'xml', report?: IBundle) {
    if (!this.data) return;

    let content = JSON.stringify(report ? report : this.data, null, '\t');

    if (format === 'xml') {
      content = await this.http.post('/convert', content, { responseType: "text" }).toPromise();
    }

    const blob = new Blob([content], { type: 'text/plain;charset=utf-8'});
    const fileName = format === 'xml' ? 'report.xml' : 'report.json';

    saveAs(blob, fileName);
  }

  async loadData() {
    this.loading = true;
    this.message = '';
    this.error = '';

    let timeout: any;
    const timeoutEvent = () => {
      this.loadingCount = this.loadingCount + 5;
      if (this.loadingCount > 100) {
        this.loadingCount = 0;
      }
      timeout = setTimeout(timeoutEvent, 300);
    };

    try {
      timeoutEvent();
      const response = <ClientReportResponse> await this.http.get('client-report', {responseType: "json"}).toPromise();
      this.data = JSON.parse(response.bundle);
      this.coordinates = response.positions;
      this.reportTableInfo = (this.data.entry || []).map(e => new ReportTableInfo(<IBundle> e.resource));
    } catch (ex) {
      this.error = ex.message;
    } finally {
      this.loading = false;
      clearTimeout(timeout);
    }
  }

  async ngOnInit() {
    if (!this.data) {
      this.loadData();
    }
  }
}
