import {Component, Input, OnInit, Output} from '@angular/core';
import {Parameter, QueryResourceParams, USCoreConfig} from "../../models/config";
import {faAdd, faRemove} from '@fortawesome/free-solid-svg-icons';
import {EnvironmentService} from "../../environment.service";
import {Subject} from "rxjs";

@Component({
  selector: 'app-query-uscore',
  templateUrl: './uscore.component.html',
  styleUrls: ['./uscore.component.css']
})
export class UscoreComponent implements OnInit {
  @Input() usCoreConfig: USCoreConfig;
  @Output() change = new Subject();

  queryParameters: Map<string, string> = new Map();
  entries?: any[];
  faRemove = faRemove;
  faAdd = faAdd;

  constructor(public envService: EnvironmentService) {
  }

  trackByFn(index: number) {
    return index;
  }

  ngOnInit(): void {
    if (!(this.usCoreConfig.queryParameters instanceof Map)) {
      const map = new Map(Object.entries(this.usCoreConfig.queryParameters));
      this.entries = Array.from(map.entries());
      this.usCoreConfig.queryParameters = map;
    } else {
      this.entries = Array.from(this.usCoreConfig.queryParameters.entries());
    }
    console.log(this.entries);
  }

  addBundle() {
    if (this.entries) {
      this.usCoreConfig.queryParameters = new Map(this.entries.map(key => [key[0], key[1]]));
    } else {
      this.usCoreConfig.queryParameters = new Map();
    }
    let queryResourceArray: QueryResourceParams[] = [];
    this.addResource(queryResourceArray)
    this.usCoreConfig.queryParameters.set('', queryResourceArray);
    this.entries = Array.from(this.usCoreConfig.queryParameters.entries());
  }

  addResource(entry: QueryResourceParams[]) {
    let queryResource = new QueryResourceParams();
    queryResource.parameters = [];
    this.addParameter(queryResource.parameters);
    queryResource.resourceType = '';
    entry.push(queryResource);
  }

  addParameter(entry: Parameter[]) {
    let parameter = new Parameter();
    parameter.name = '';
    parameter.values = [''];
    entry.push(parameter);
  }

  addValue(entry: string[]) {
    entry.push("");
  }

  deleteBundle(index: number) {
    this.entries.splice(index, 1);
    this.usCoreConfig.queryParameters = new Map(this.entries.map(key => [key[0], key[1]]));
    this.envService.saveEnvEvent.next(null);
  }

  deleteResource(resource: QueryResourceParams[], index: number) {
    resource.splice(index, 1);
    this.usCoreConfig.queryParameters = new Map(this.entries.map(key => [key[0], key[1]]));
    this.envService.saveEnvEvent.next(null);
  }

  deleteParameter(resource: Parameter[], index: number) {
    resource.splice(index, 1);
    this.usCoreConfig.queryParameters = new Map(this.entries.map(key => [key[0], key[1]]));
    this.envService.saveEnvEvent.next(null);
  }

  deleteValue(resource: string[], index: number) {
    resource.splice(index, 1);
    this.usCoreConfig.queryParameters = new Map(this.entries.map(key => [key[0], key[1]]));
    this.envService.saveEnvEvent.next(null);
  }


  onChange() {
    this.usCoreConfig.queryParameters = new Map(this.entries.map(key => [key[0], key[1]]));
    this.envService.saveEnvEvent.next(null);
  }

}
