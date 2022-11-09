import {Component, Input, OnInit} from '@angular/core';
import {ApiConfig, MeasurePackagesConfig} from 'src/app/models/config';
import {EnvironmentService} from "../../environment.service";
import {faAdd, faRemove} from '@fortawesome/free-solid-svg-icons';

@Component({
  selector: 'app-multi-measure',
  templateUrl: './multi-measure.component.html',
  styleUrls: ['./multi-measure.component.css']
})
export class MultiMeasureComponent implements OnInit {

  @Input() apiConfig: ApiConfig;
  faRemove = faRemove;
  faAdd = faAdd;
  measurePackages: MeasurePackagesConfig[] = [];

  constructor(public envService: EnvironmentService) { }

  onChange() {

    this.apiConfig.measurePackages = this.measurePackages;

    // Save the environment
    this.envService.saveEnvEvent.next(null);
  }

  onClick(){
    console.log("clicked");
    let bundleIds: string[] = [];
    let multiMeasureConfig = {"id": '', "bundleIds": bundleIds}
    this.apiConfig.measurePackages.push(multiMeasureConfig);
  }


  trackByMethod(index:number, el:any): number {
    return index;
  }

  ngOnInit(): void {
    if (this.apiConfig.measurePackages) {
      this.measurePackages = this.apiConfig.measurePackages || [];
    }
  }

}
