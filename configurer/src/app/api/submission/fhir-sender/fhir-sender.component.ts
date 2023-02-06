import {Component, Input, OnInit} from '@angular/core';
import {FHIRSenderConfigWrapper} from "../../../models/config";
import {EnvironmentService} from "../../../environment.service";

@Component({
  selector: 'app-submission-fhir-sender',
  templateUrl: './fhir-sender.component.html',
  styleUrls: ['./fhir-sender.component.css']
})
export class FhirSenderComponent implements OnInit {
  @Input() senderConfigWrapper: FHIRSenderConfigWrapper;

  constructor(public envService: EnvironmentService) { }

  ngOnInit(): void {
  }

}
