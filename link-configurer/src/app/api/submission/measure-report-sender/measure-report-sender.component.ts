import {Component, Input, OnInit} from '@angular/core';
import {FHIRSenderConfigWrapper} from "../../../models/config";
import {EnvironmentService} from "../../../environment.service";

@Component({
  selector: 'app-submission-measure-report-sender',
  templateUrl: './measure-report-sender.component.html',
  styleUrls: ['./measure-report-sender.component.css']
})
export class MeasureReportSenderComponent implements OnInit {
  @Input() senderConfigWrapper: FHIRSenderConfigWrapper;

  constructor(public envService: EnvironmentService) { }

  ngOnInit(): void {
  }

}
