import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {AuthService} from 'src/services/auth/auth.service';
import {AppConfigService} from "../app.config";

@Injectable({ providedIn: 'root' })
export class DataService {

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    public appConfigService: AppConfigService) {
  }

  private createHeaders() {
    return new HttpHeaders({
      'Authorization': `Bearer ${sessionStorage.getItem('access_token')}`
    });
  }

  private getApiBaseUrl(): string {
    const config = this.appConfigService.getConfig();
    if (config && config.apiBaseUrl) {
      return config.apiBaseUrl;
    }
    throw new Error('API base URL is not set');
  }

  private constructApiUrl(resource: string) {
    const baseUrl = this.getApiBaseUrl()
    return `${baseUrl}/${resource}`
  }

  private performRequest<T>(method: 'get' | 'post' | 'put', resource: string, data?: T) {
    if (!this.authService.isLoggedIn()) {
      throw new Error('User is not authenticated or token is invalid');
    }

    const url = this.constructApiUrl(resource)
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
