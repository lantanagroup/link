import {Injectable} from '@angular/core';
import {WebConfig} from "./models/web-config";
import {ConfigTypes} from "./models/config";
import {Router} from "@angular/router";
import {HttpClient} from "@angular/common/http";
import {debounceTime, Subject} from "rxjs";
import {ToastService} from "./toast.service";
import {ApiConfigWrapper, ConsumerConfigWrapper} from "./models/config-wrappers";
import {YamlPipe} from './yaml.pipe';
import {stringify} from 'yaml';

@Injectable({
  providedIn: 'root'
})
export class EnvironmentService {
  apiConfig?: ApiConfigWrapper;
  webConfig?: WebConfig;
  consumerConfig?: ConsumerConfigWrapper;
  saveEnvEvent = new Subject<void>();


  constructor(private router: Router, private http: HttpClient, private toastService: ToastService) {
    this.init();
    this.saveEnvEvent.pipe(debounceTime(1000))
      .subscribe(async () => {
        await this.saveConfig('api');
        await this.saveConfig('consumer');
        await this.saveConfig('web');
        this.toastService.show('Saved!', 'Saved File ', 2000);
      });
  }


  init() {
   this.readConfig('api');
   this.readConfig('consumer');
   this.readConfig('web');
  }

  async readConfig(configType: ConfigTypes){
    let data
    switch (configType) {
      case 'api':
        data = await this.http.get<ApiConfigWrapper>('/api/config/' + configType).toPromise();
        if (data) {
          let filter = new YamlPipe();
          filter.objectToCamelCase(data);
          this.apiConfig = data;
        }
       break;
      case 'consumer':
        data = await this.http.get<ConsumerConfigWrapper>('/api/config/' + configType).toPromise();
        if (data) {
          let filter = new YamlPipe();
          filter.objectToCamelCase(data);
          this.consumerConfig = data;
        }
        break;
      case 'web':
        data = await this.http.get<WebConfig>('/api/config/' + configType).toPromise();
        if (data) {
          let filter = new YamlPipe();
          filter.objectToCamelCase(data);
          this.webConfig = data;
        }
    }
    return data;
}


  async addConfig(configType: ConfigTypes) {
      switch (configType) {
        case 'api':
          this.apiConfig = new ApiConfigWrapper();
          await this.saveConfig('api');
          await this.router.navigate(['/api']);
          break;
        case 'web':
         this.webConfig = new WebConfig();
         await this.saveConfig('web');
          await this.router.navigate(['/web']);
          break;
        case 'consumer':
          this.consumerConfig = new ConsumerConfigWrapper();
          await this.saveConfig('consumer');
          await this.router.navigate(['/consumer']);
          break;
      }
  }

  async saveConfig(configType: ConfigTypes) {
    let filter = new YamlPipe();
    let clone;
    switch (configType) {
      case 'api':
        clone = JSON.parse(JSON.stringify(this.apiConfig));
        break;
      case 'consumer':
        clone = JSON.parse(JSON.stringify(this.consumerConfig));
        break;
      case 'web':
          clone = JSON.parse(JSON.stringify(this.webConfig));
          break;
    }
    filter.objectToSnakeCase(clone);
    let yamlApi = stringify(clone, { version: '1.2' });
    const data = {
      api :  yamlApi
    }
    await this.http.post('/api/config/' + configType, data).toPromise();
  }

  async removeConfig(configType: ConfigTypes) {
    try {
      await this.http.delete('/api/config/' + configType).toPromise();
      this.init();
    } catch (ex: any) {
      this.toastService.show('Error removing environment', ex);
    }
  }
}
