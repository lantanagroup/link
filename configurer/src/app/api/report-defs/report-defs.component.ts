import {Component, Input, OnInit} from '@angular/core';
import {ApiConfig, ApiReportDefsConfig} from "../../models/config";
import {EnvironmentService} from "../../environment.service";
import {faAdd, faRemove} from '@fortawesome/free-solid-svg-icons';

@Component({
  selector: 'app-report-defs',
  templateUrl: './report-defs.component.html',
  styleUrls: ['./report-defs.component.css']
})
export class ReportDefsComponent implements OnInit {
  @Input() apiConfig: ApiConfig;
  reportDefs = new ApiReportDefsConfig();
  faRemove = faRemove;
  faAdd = faAdd;

  constructor(public envService: EnvironmentService) { }

  onChange() {
    // If the cors property hasn't been defined on apiConfig yet because it's new, make sure it gets set here before saving the environment
    this.apiConfig.reportDefs = this.reportDefs;

    // Save the environment
    this.envService.saveEnvEvent.next(null);
  }

  ngOnInit(): void {
    if (this.apiConfig.reportDefs) {
      this.reportDefs = this.apiConfig.reportDefs;
    }
  }
}
