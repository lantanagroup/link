import {Component, OnInit} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import saveAs from 'save-as';
import {google} from 'google-maps';
import {ReportTableInfo} from "./report-table-info";
import {IBundle} from "./fhir";

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
    strength: number;
  }[];
  reportTableInfo: ReportTableInfo[];

  constructor(private http: HttpClient) {
    if (localStorage.getItem('lastData')) {
      try {
        this.data = JSON.parse(localStorage.getItem('lastData'));
        this.http.post('heatmap', JSON.stringify(this.data), {responseType: 'json'}).toPromise()
          .then(coordinates => this.coordinates = <any> coordinates);
        this.reportTableInfo = (this.data.entry || []).map(e => new ReportTableInfo(<IBundle>e.resource));
        this.message = 'Displaying cached data. Press "Reload Report" to refresh data.';
      } catch (ex) {}
    }
  }

  onMapLoad(mapInstance: google.maps.Map) {
    const data = [];

    this.coordinates.forEach(c => {
      for (let i = 0; i < c.strength; i++) {
        data.push(new google.maps.LatLng(c.latitude, c.longitude));
      }
    });
    new google.maps.visualization.HeatmapLayer({
      map: <any> mapInstance,
      data: data,

    });
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
      this.data = <any> await this.http.get('case-report-bundle.json', {responseType: "json"}).toPromise();
      this.coordinates = <any> await this.http.post('heatmap', JSON.stringify(this.data), {responseType: 'json'}).toPromise();
      this.reportTableInfo = (this.data.entry || []).map(e => new ReportTableInfo(<IBundle> e.resource));

      localStorage.setItem('lastData', JSON.stringify(this.data));
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
