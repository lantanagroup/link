import {HttpClient} from "@angular/common/http";
import {Injectable} from "@angular/core";
import {firstValueFrom} from "rxjs";

export interface AppConfig {
  authAccountUrl: string;
  authOpenIdUrl: string;
  authClientId: string;
}

//AppConfigService.ts
@Injectable({
  providedIn: 'root'
})
export class AppConfigService {
  loaded = false;
  private config?: AppConfig;

  constructor(private http: HttpClient) {
  }

  async loadConfig(): Promise<void> {
    const data = await firstValueFrom(this.http.get<AppConfig>('/assets/app.config.json'));
    if (data) {
      this.config = this.checkConfig(data);
    }
    this.loaded = true;
  }

  private removeTrailingSlash(url: string) {
    if (url && url.endsWith('/') && url.length > 1) {
      return url.replace(/\/+$/, '');
    }
    return url;
  }

  private checkConfig(appConfig: AppConfig) {
    if (!appConfig) {
      return appConfig;
    }

    appConfig.apiBaseUrl = this.removeTrailingSlash(appConfig.apiBaseUrl);
    appConfig.authAccountUrl = this.removeTrailingSlash(appConfig.authAccountUrl);
    appConfig.authOpenIdUrl = this.removeTrailingSlash(appConfig.authOpenIdUrl);
    return appConfig;
  }

  getConfig() {
    return this.config;
  }
}
