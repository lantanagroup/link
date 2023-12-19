import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, FormArray, FormBuilder, Validators, Form } from '@angular/forms';
import { ReactiveFormsModule } from '@angular/forms';
import { SectionHeadingComponent } from '../section-heading/section-heading.component';
import { AccordionComponent } from '../accordion/accordion.component';
import { ButtonComponent } from '../button/button.component';
import { IconComponent } from '../icon/icon.component';

@Component({
  selector: 'app-form-update-facility',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SectionHeadingComponent, AccordionComponent, ButtonComponent, IconComponent],
  templateUrl: './form-update-facility.component.html',
  styleUrls: ['./form-update-facility.component.scss']
})
export class FormUpdateFacilityComponent {

  @Input() facilityID?: string | null = null;

  constructor(private fb: FormBuilder) {}

  facilitiesForm = new FormGroup({
    profile: new FormGroup({
      tenantId: new FormControl('', [Validators.required]),
      name: new FormControl('', [Validators.required]),
      description: new FormControl(''),
      bundleName: new FormControl('', [Validators.required]),
      cdcOrgId: new FormControl(''),
      database: new FormControl(''),
      vendor: new FormControl(''),
      dataRetentionPeriod: new FormControl('')
    }),
    normalizations: new FormGroup({
      codeSystemCleanup: new FormControl(0),
      containedResourceCleanup: new FormControl(0),
      copyLocation: new FormControl(0),
      encounterStatus: new FormControl(0),
      fixPeriodDates: new FormControl(0),
      fixResourceIds: new FormControl(0),
      patientDataResource: new FormControl(0)
    }),
    conceptMaps: this.fb.array([]),
    scheduling: new FormGroup({
      queryPatientList: new FormControl(''),
      dataRetentionCheck: new FormControl(''),
      schedules: this.fb.array([],[Validators.required])
    }),
    nativeFHIRQuery: new FormGroup({
      nativeFHIREndpoint: new FormControl(''),
      parallelPatients: new FormControl(),
      authenticationMethod: new FormControl('None')
    }),
    censusAcquisition: new FormGroup({
      method: new FormControl('')
    }),
    bulkFHIR: new FormGroup({
      bulkFHIREndpoint: new FormControl(''),
      waitInterval: new FormControl(),
      groupID: new FormControl(''),
      initialResponseURLHeader: new FormControl(''),
      progressHeaderName: new FormControl(''),
      progressCompleteHeaderValue: new FormControl('')
    }),
    queryPlans: this.fb.array([])
  });

  // Concept Maps Dynamic Fields
  get conceptMaps(): FormArray {
    return this.facilitiesForm.get('conceptMaps') as FormArray;
  }

  createConceptMap(): FormGroup {
    return this.fb.group({
      id: new FormControl(''),
      name: new FormControl(''),
      contexts: new FormControl(''),
      map: new FormControl('')
    });
  }

  addConceptMap() {
    this.conceptMaps.push(this.createConceptMap());
  }

  getAddConceptMapHandler(): () => void {
    return () => this.addConceptMap()
  }

  removeConceptMap(index: number) {
    this.conceptMaps.removeAt(index);
  }

  getRemoveConceptMapHandler(index: number): () => void {
    return () => this.removeConceptMap(index)
  }

  // Schedules Dynamic Fields
  get schedules(): FormArray {
    return this.facilitiesForm.get('scheduling.schedules') as FormArray;
  }

  createSchedule(): FormGroup {
    return this.fb.group({
      measureIds: new FormControl(''),
      reportingPeriod: new FormControl('Last Month'),
      schedule: new FormControl(''),
      regenerate: new FormControl(1)
    });
  }

  addSchedule() {
    this.schedules.push(this.createSchedule());
  }

  getAddScheduleHandler(): () => void {
    return () => this.addSchedule()
  }

  removeSchedule(index: number) {
    this.schedules.removeAt(index);
  }

  getRemoveScheduleHandler(index: number): () => void {
    return () => this.removeSchedule(index)
  }

  // Query Plan Dynamic Fields
  get queryPlans(): FormArray {
    return this.facilitiesForm.get('queryPlans') as FormArray;
  }

  createQueryPlan(): FormGroup {
    return this.fb.group({
      measureId: new FormControl(''),
      plan: new FormControl('')
    });
  }

  addQueryPlan() {
    this.queryPlans.push(this.createQueryPlan());
  }

  getAddQueryPlanHandler(): () => void {
    return () => this.addQueryPlan()
  }

  removeQueryPlan(index: number) {
    this.queryPlans.removeAt(index);
  }

  getRemoveQueryPlanHandler(index: number): () => void {
    return () => this.removeQueryPlan(index)
  }

  // handle submit

  onSubmit() {
    if (this.facilitiesForm.valid) {
      alert(JSON.stringify(this.facilitiesForm.value))
    } else {
      this.facilitiesForm.markAllAsTouched()
    }
  }
}
