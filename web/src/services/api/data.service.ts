import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from 'src/services/auth/auth.service';

@Injectable({ providedIn: 'root' })
export class DataService {
  private API_BASE_URL: string = 'https://dev.nhsnlink.org/api'; // Set the base URL

  constructor(private http: HttpClient, private authService: AuthService) {}

  private createHeaders() {
    return new HttpHeaders({
      'Authorization': `Bearer ${sessionStorage.getItem('access_token')}`
    });
  }

  private performRequest<T>(method: 'get' | 'post' | 'put', resource: string, data?: T) {
    if (!this.authService.isLoggedIn()) {
      throw new Error('User is not authenticated or token is invalid');
    }

    const url = `${this.API_BASE_URL}/${resource}`;
    const options = { headers: this.createHeaders() };

    switch (method) {
      case 'get':
        return this.http.get<T>(url, options);
      case 'post':
        return this.http.post<T>(url, data, options);
      case 'put':
        return this.http.put<T>(url, data, options);
      default:
        throw new Error('Invalid HTTP method');
    }
  }

  getData<T>(resource: string) {
    return this.performRequest<T>('get', resource);
  }

  postData<T>(resource: string, data: T) {
    return this.performRequest<T>('post', resource, data);
  }

  putData<T>(resource: string, data: T) {
    return this.performRequest<T>('put', resource, data);
  }
}