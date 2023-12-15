import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class DataService {
  constructor(private http: HttpClient) {}

  // Existing method for getting protected data
  getProtectedData() {
    // debugger;
    const token = sessionStorage.getItem('access_token');
    // console.log(token);
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
    return this.http.get('https://dev.nhsnlink.org/api/info', { headers });
  }

  // POST METHODS
  createData(data: any) {
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${sessionStorage.getItem('access_token')}`
    });
    return this.http.post('your-api-post-url', data, { headers });
  }

  // Additional methods for other API calls
}
