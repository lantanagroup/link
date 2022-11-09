import {Component, Input, OnInit, Output} from '@angular/core';
import {
  QueryAuth,
  QueryAuthAzure,
  QueryAuthBasic,
  QueryAuthCerner,
  QueryAuthEpic,
  QueryAuthToken,
  QueryConfig,
  USCoreConfig
} from "../models/config";
import {EnvironmentService} from "../environment.service";
import {Subject} from "rxjs";
import {faRemove} from '@fortawesome/free-solid-svg-icons';

@Component({
  selector: 'app-query',
  templateUrl: './query.component.html',
  styleUrls: ['./query.component.css']
})
export class QueryComponent implements OnInit {
  @Input() queryConfig: QueryConfig;
  @Input() configWrapper?: any;
  @Output() change = new Subject();
  @Input() includeRemoteProperties = false;
  faRemove = faRemove;

  constructor(public envService: EnvironmentService) { }

  trackByFn(index: number) {
    return index;
  }

  onChange() {
    this.change.next(null);
  }

  onChangeAuth() {
    if (!this.queryConfig.auth) {
      this.queryConfig.auth = new QueryAuth();
    }

    if (this.queryConfig.authClass === 'com.lantanagroup.link.query.AzureAuth') {
      this.queryConfig.auth.azure = new QueryAuthAzure();
      delete this.queryConfig.auth.cerner;
      delete this.queryConfig.auth.basic;
      delete this.queryConfig.auth.epic;
      delete this.queryConfig.auth.token;
    } else if (this.queryConfig.authClass === 'com.lantanagroup.link.query.EpicAuth') {
      this.queryConfig.auth.epic = new QueryAuthEpic();
      delete this.queryConfig.auth.cerner;
      delete this.queryConfig.auth.basic;
      delete this.queryConfig.auth.azure;
      delete this.queryConfig.auth.token;
    } else if (this.queryConfig.authClass === 'com.lantanagroup.link.query.CernerAuth') {
      this.queryConfig.auth.cerner = new QueryAuthCerner();
      delete this.queryConfig.auth.azure;
      delete this.queryConfig.auth.basic;
      delete this.queryConfig.auth.epic;
      delete this.queryConfig.auth.token;
    } else if (this.queryConfig.authClass === 'com.lantanagroup.link.query.BasicAuth') {
      this.queryConfig.auth.basic = new QueryAuthBasic();
      delete this.queryConfig.auth.cerner;
      delete this.queryConfig.auth.azure;
      delete this.queryConfig.auth.epic;
      delete this.queryConfig.auth.token;
    } else if (this.queryConfig.authClass === 'com.lantanagroup.link.query.TokenAuth') {
      this.queryConfig.auth.token = new QueryAuthToken();
      delete this.queryConfig.auth.cerner;
      delete this.queryConfig.auth.basic;
      delete this.queryConfig.auth.epic;
      delete this.queryConfig.auth.azure;
    }

    this.onChange();
  }

  queryClassChanged() {
    if (this.queryConfig.queryClass === 'com.lantanagroup.link.query.uscore.Query') {
      this.configWrapper.uscore = new USCoreConfig();
    } else {
      delete this.configWrapper.uscore;
    }

    this.onChange();
  }

  ngOnInit(): void {
  }

}
