import {Component, OnInit} from '@angular/core';
import {EnvironmentService} from "../environment.service";
import {Router} from "@angular/router";
import {FHIRSenderConfigWrapper} from "../models/config";

@Component({
  selector: 'app-api',
  templateUrl: './api.component.html',
  styleUrls: ['./api.component.css']
})
export class ApiComponent implements OnInit {
  constructor(public envService: EnvironmentService, private router: Router) { }

  downloaderOptions = [
    'com.lantanagroup.link.nhsn.MeasureReportDownloader'
  ];
  senderOptions = [
    'com.lantanagroup.link.nhsn.FHIRSender',
    'com.lantanagroup.link.thsa.MeasureReportSender'
  ];
  patientIdResolverOptions = [
    'com.lantanagroup.link.nhsn.StoredListProvider'
  ];

  eventNames = [
    'beforeMeasureResolution',
    'afterMeasureResolution',
    'onRegeneration',
    'beforePatientOfInterestLookup',
    'afterPatientOfInterestLookup',
    'beforePatientDataQuery',
    'afterPatientDataQuery',
    'beforePatientDataStore',
    'afterPatientDataStore',
    'beforeMeasureEval',
    'beforeReportStore',
    'afterReportStore',
    'beforeBundling',
    'afterBundling'
  ];

  trackByMethod(index:number, el:any): number {
    return index;
  }

  senderChanged() {
    if (!this.envService.apiConfig.api.sender) {
      return;
    }

    if (this.envService.apiConfig.api.sender.endsWith('.nhsn.FHIRSender') || this.envService.apiConfig.api.sender.endsWith('.thsa.MeasureReportSender')) {
      if (!this.envService.apiConfig.sender) {
        this.envService.apiConfig.sender = new FHIRSenderConfigWrapper();
      }
    }

    this.envService.saveEnvEvent.next(null);
  }

  async ngOnInit() {
    if (!this.envService.apiConfig) {
      await this.router.navigate(['/']);
    }
  }
}
