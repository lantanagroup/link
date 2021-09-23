import {Component, Input, OnInit} from '@angular/core';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {ReportService} from '../services/report.service';
import {ToastService} from '../toast.service';
import {PatientDataModel} from "../model/PatientDataModel";
import {Condition, EncounterLocationComponent} from "../model/fhir";
import {getFhirDate} from '../helper';

@Component({
  selector: 'ViewPatientComponent',
  templateUrl: './view-patient.component.html'
})
export class ViewPatientComponent implements OnInit {
  @Input() patientId: string;
  @Input() reportId: string;

  public patientData: PatientDataModel;
  public loading = false;
  condition

  constructor(
      public activeModal: NgbActiveModal,
      private reportService: ReportService,
      private toastService: ToastService) {
  }

  public async refresh() {
    this.loading = true;
    try {
      this.patientData = await this.reportService.getPatientData(this.reportId, this.patientId);
    } catch (ex) {
      this.toastService.showException('Error loading line-level data', ex);
    } finally {
      this.loading = false;
    }
  }

  public getLocationData(location: EncounterLocationComponent) {
    let locationData = "";
    if (location.period && location.period.start) {
      locationData = getFhirDate(location.period.start);
      if (location.period && location.period.end) {
        locationData = locationData + " - " + getFhirDate(location.period.end) + ": ";
      } else {
        locationData = locationData + " - current: ";
      }
    }
    locationData = locationData + location.location.display;
    return locationData;
  }

  getOnSet(condition: Condition) {
    if (condition.hasOwnProperty('onsetDateTime')) {
      const fhirDateTime = condition['onsetDateTime'] as string
      return getFhirDate(fhirDateTime);
    } else return "";
  }

  getAbatement(condition: Condition) {
    if (condition.hasOwnProperty('abatementDateTime')) {
      const fhirDateTime = condition['abatementDateTime'] as string
      return getFhirDate(fhirDateTime);
    } else return "";
  }

  async ngOnInit() {
    await this.refresh();
  }

}
