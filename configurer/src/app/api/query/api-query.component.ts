import {Component, Input, OnInit} from '@angular/core';
import {ApiConfig, ApiQueryConfig, QueryConfig} from "../../models/config";
import {EnvironmentService} from "../../environment.service";
import {ApiConfigWrapper} from "../../models/config-wrappers";

@Component({
  selector: 'app-api-query',
  templateUrl: './api-query.component.html',
  styleUrls: ['./api-query.component.css']
})
export class ApiQueryComponent implements OnInit {
  @Input() apiConfigWrapper: ApiConfigWrapper;
  apiConfig: ApiConfig;
  queryConfig: QueryConfig = new QueryConfig();

  constructor(public envService: EnvironmentService) { }

  get queryMode() {
    if (!this.apiConfig.query) { return null; }
    return this.apiConfig.query.mode;
  }

  set queryMode(value: 'Local'|'Remote') {
    if (!this.apiConfig.query) {
      this.apiConfig.query = new ApiQueryConfig();
    }
    this.apiConfig.query.mode = value;
  }

  get queryUrl() {
    if (!this.apiConfig.query) { return ''; }
    return this.apiConfig.query.url;
  }

  set queryUrl(value: string) {
    if (!this.apiConfig.query) {
      this.apiConfig.query = new ApiQueryConfig();
    }
    this.apiConfig.query.url = value;
  }

  get queryApiKey() {
    if (!this.apiConfig.query) { return ''; }
    return this.apiConfig.query.apiKey;
  }

  set queryApiKey(value: string) {
    if (!this.apiConfig.query) {
      this.apiConfig.query = new ApiQueryConfig();
    }
    this.apiConfig.query.apiKey = value;
  }

  modeChanged() {
    if (this.apiConfig.query.mode === 'Local') {
      if (this.apiConfig.query) {
        delete this.apiConfig.query.url;
        delete this.apiConfig.query.apiKey;
      }

      if (!this.apiConfigWrapper.query) {
        this.apiConfigWrapper.query = new QueryConfig();
      }
    } else if (this.apiConfig.query.mode === 'Remote') {
      delete this.apiConfigWrapper.query;
    }

    this.envService.saveEnvEvent.next(null);
  }

  onChangeQueryConfig() {
    this.apiConfigWrapper.query = this.queryConfig;
    this.envService.saveEnvEvent.next(null);
  }

  ngOnInit(): void {
  //  this.apiConfig = this.apiConfigWrapper.api || new ApiConfig();
    this.queryConfig = this.apiConfigWrapper.query || new QueryConfig();
  }
}
