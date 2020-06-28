import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {ConfigService} from './config.service';
import {LocationResponse} from '../model/location-response';


@Injectable()
export class LocationService {
  constructor(private http: HttpClient, private configService: ConfigService) {
  }

  async search(searchText?: string, identifierText?: string) {
    let url = '/api/location?';

    if (searchText) {
      url += `search=${encodeURIComponent(searchText)}&`;
    }

    if (identifierText) {
      url += `identifier=${encodeURIComponent(identifierText)}&`;
    }

    url = this.configService.getApiUrl(url);

    return this.http.get<LocationResponse[]>(url).toPromise();
  }
}