import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {IConfig} from '../model/config';
import {ApiInfoModel} from '../model/api-info-model';

@Injectable({
  providedIn: 'root'
})
export class ConfigService {
  config: IConfig;

  constructor(private http: HttpClient) {
  }

  getApiUrl(urlPart: string) {
    return this.config.apiUrl +
        (this.config.apiUrl.endsWith('/') ? '' : '/') +
        (urlPart.startsWith('/') ? urlPart.substring(1) : urlPart);
  }

  async loadOverrideConfig() {
    try {
      const overrideConfig = await this.http.get<IConfig>('./assets/local.json').toPromise();
      Object.assign(this.config, overrideConfig);
    } catch {
      // Never throw error if there is a problem loading the override config
    }
  }

  async loadConfig() {
    this.config = await this.http.get<IConfig>('./assets/config.json').toPromise();
    await this.loadOverrideConfig();
  }

  async getApiInfo() {
    return await this.http.get<ApiInfoModel>(this.config.apiUrl).toPromise();
  }

}
