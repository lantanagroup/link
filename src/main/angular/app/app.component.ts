import {Component, OnInit} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import saveAs from 'save-as';
import {google} from 'google-maps';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  loading = false;
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

  constructor(private http: HttpClient) {

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

  async download(format: 'json'|'xml') {
    if (!this.data) return;

    let content = JSON.stringify(this.data, null, '\t');

    if (format === 'xml') {
      content = await this.http.post('/convert', content, { responseType: "text" }).toPromise();
    }

    const blob = new Blob([content], { type: 'text/plain;charset=utf-8'});
    const fileName = format === 'xml' ? 'report.xml' : 'report.json';

    saveAs(blob, fileName);
  }

  async loadData() {
    this.loading = true;

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
      console.log(this.coordinates);
    } catch (ex) {
      this.message = ex.message;
    } finally {
      this.loading = false;
      clearTimeout(timeout);
    }
  }

  async ngOnInit() {
    this.loadData();
  }
}
