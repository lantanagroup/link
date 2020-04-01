import { Component } from '@angular/core';
import {environment} from "../environments/environment";
import {HttpClient} from "@angular/common/http";
import saveAs from 'save-as';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  loading = false;
  message: string;

  constructor(private http: HttpClient) {

  }

  async generate(format: 'json'|'xml') {
    let url;

    switch (format) {
      case 'json':
        url = 'case-report-bundle.json';
        break;
      case 'xml':
        url = 'case-report-bundle.xml';
        break;
    }

    this.loading = true;

    try {
      const results = await this.http.get(url, {responseType: "blob"}).toPromise();
      saveAs(results, url);
    } catch (ex) {
      this.message = ex.message;
    } finally {
      this.loading = false;
    }
  }
}
