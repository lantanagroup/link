import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, FormArray, FormBuilder, Validators, Form } from '@angular/forms';
import { ReactiveFormsModule } from '@angular/forms';
import { SectionHeadingComponent } from '../section-heading/section-heading.component';
import { AccordionComponent } from '../accordion/accordion.component';
import { ButtonComponent } from '../button/button.component';
import { IconComponent } from '../icon/icon.component';
import { ToastComponent } from '../toast/toast.component';
import { ToastService } from 'src/app/services/toast.service';
import { Router } from '@angular/router';
import { FacilitiesApiService } from 'src/services/api/facilities/facilities-api.service';

@Component({
  selector: 'app-form-update-facility',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SectionHeadingComponent, AccordionComponent, ButtonComponent, IconComponent, ToastComponent],
  templateUrl: './form-update-facility.component.html',
  styleUrls: ['./form-update-facility.component.scss']
})
export class FormUpdateFacilityComponent {

  @Input() facilityId?: string | null = null;

  constructor(private fb: FormBuilder, private facilitiesApiService: FacilitiesApiService, private toastService: ToastService, private router: Router) { }
  facilityDetails: any = null;
  isDataLoaded: boolean = false;

  async GetFacilityDetails(id: string) {
    try {
      const tenantDetail = await this.facilitiesApiService.fetchFacilityById(id);
      this.facilityDetails = tenantDetail;
      this.isDataLoaded = true;
    } catch (error) {
      console.error('Error Loading table data.', error);
    }
  }

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
      schedules: this.fb.array([], [Validators.required])
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

  // update with initial form values

  async ngOnInit(): Promise<void> {
    if (this.facilityId) {
      await this.setInitialValues(this.facilityId);
    } else {
      this.isDataLoaded = true;
    }
  }


  private async setInitialValues(facilityId: string) {
    const tenantDetail = await this.facilitiesApiService.fetchFacilityById(facilityId);

    // Schedules
    if (tenantDetail?.scheduling?.generateAndSubmitReports) {
      for (let i = 0; i < tenantDetail.scheduling.generateAndSubmitReports.length; i++) {
        this.addSchedule();
      }
    }

    // Concept Maps
    if (tenantDetail?.conceptMaps) {
      for (const cm of tenantDetail.conceptMaps) {
        this.addConceptMap();
      }
    }

    // Query Plans
    if (tenantDetail?.queryPlans) {
      for (const qp of tenantDetail.queryPlans) {
        this.addQueryPlan();
      }
    }

    this.facilitiesForm.patchValue({

      profile: {
        tenantId: this.facilityId,
        name: tenantDetail?.name,
        description: 'This is placeholder content that we assign because there is an ID passed into this form', // TODO:CheckClient
        bundleName: tenantDetail?.bundling?.name,
        cdcOrgId: tenantDetail?.cdcOrgId,
        database: tenantDetail?.connectionString,
        vendor: tenantDetail?.vendor, // TODO:CheckClient
        dataRetentionPeriod: tenantDetail?.retentionPeriod,
      },
      normalizations: {
        codeSystemCleanup: tenantDetail?.normalizations?.codeSystemCleanup,
        containedResourceCleanup: tenantDetail?.normalizations?.containedResourceCleanup,
        copyLocation: tenantDetail?.normalizations?.copyLocation,
        encounterStatus: tenantDetail?.normalizations?.encounterStatus,
        fixPeriodDates: tenantDetail?.normalizations?.fixPeriodDates,
        fixResourceIds: tenantDetail?.normalizations?.fixResourceIds,
        patientDataResource: tenantDetail?.normalizations?.patientDataResource
      },
      conceptMaps: tenantDetail?.conceptMaps || [],
      scheduling: this.mapSchedulingApiDataToFormData(tenantDetail?.scheduling),
      nativeFHIRQuery: {
        // nativeFHIREndpoint: tenantDetail.fhirQuery.fhirServerBase, // TODO:
        // parallelPatients: tenantDetail.fhirQuery.parallelPatients, // TODO:
        // authenticationMethod: tenantDetail.fhirQuery.authClass // TODO:
      },
      censusAcquisition: {
        method: tenantDetail?.censusAcquisition?.method
      },
      bulkFHIR: {
        bulkFHIREndpoint: tenantDetail?.bulkFHIR?.bulkFHIREndpoint,
        waitInterval: tenantDetail?.bulkFHIR?.waitInterval,
        groupID: tenantDetail?.bulkFHIR?.groupID,
        initialResponseURLHeader: tenantDetail?.bulkFHIR?.initialResponseURLHeader,
        progressHeaderName: tenantDetail?.bulkFHIR?.progressHeaderName,
        progressCompleteHeaderValue: tenantDetail?.bulkFHIR?.progressCompleteHeaderValue
      },
      queryPlans: tenantDetail?.queryPlans || []
    })

    this.isDataLoaded = true;
  }

  // handle submit

  async onSubmit() {
    const formData = this.facilitiesForm.value;
    const submissionData = {
      id: formData.profile?.tenantId,
      name: formData.profile?.name,
      retentionPeriod: formData.profile?.dataRetentionPeriod,
      connectionString: formData.profile?.database,
      cdcOrgId: formData.profile?.cdcOrgId,

      bundling: {
        name: formData?.profile?.bundleName
      }
      // calculate other elements as well for the request
    };
    if (this.facilitiesForm.valid) {
      try {
        let response;
        if (this.facilityId) {
          // Update existing facility
          response = await this.facilitiesApiService.updateFacility(this.facilityId, submissionData);
          // response = await this.facilitiesApiService.updateFacility(this.facilityId, this.facilitiesForm.value);

          this.toastService.showToast(
            'Facility Updated',
            `${this.facilitiesForm.value.profile?.name} has been successfully updated.`,
            'success'
          )
          this.router.navigate(['/facilities/facility', this.facilityId])
        } else {
          // Create new facility
          response = await this.facilitiesApiService.createFacility(submissionData);
          // response = await this.facilitiesApiService.createFacility(this.facilitiesForm.value);

          this.toastService.showToast(
            'Facility Created',
            `${this.facilitiesForm.value.profile?.name} has been successfully created.`,
            'success'
          )
          this.router.navigate(['/facilities/'])
        }
      } catch (error) {
        console.error('Error submitting facility data', error);
        this.toastService.showToast(
          'Facility Error',
          `Error while adding/updating ${this.facilitiesForm.value.profile?.name}.`,
          'failed'
        )
      }
      // alert(JSON.stringify(this.facilitiesForm.value))
    } else {
      this.facilitiesForm.markAllAsTouched()
    }
  }


  /// MAPPING FUNCTIONS

  // Map Scheduling Data
  mapSchedulingApiDataToFormData(schedulingData: any) {
    if (!schedulingData) {
      return {
        queryPatientList: null,
        dataRetentionCheck: null,
        schedules: []
      };
    }

    const { queryPatientListCron, dataRetentionCheckCron, generateAndSubmitReports } = schedulingData as {
      queryPatientListCron: string,
      dataRetentionCheckCron: string,
      generateAndSubmitReports: {
        cron: string,
        measureIds: string[],
        reportingPeriodMethod: string,
        regenerateIfExists: boolean
      }[]
    };

    // Transforming generateAndSubmitReports to match the form's schedule structure
    const schedules = generateAndSubmitReports.map(report => {
      return {
        measureIds: report.measureIds.join(', '),
        reportingPeriod: report.reportingPeriodMethod,
        schedule: report.cron,
        regenerate: report.regenerateIfExists ? 1 : 0
      };
    });

    // Creating the transformed scheduling object for the form
    const formSchedulingData = {
      queryPatientList: queryPatientListCron,
      dataRetentionCheck: dataRetentionCheckCron,
      schedules: schedules
    };

    return formSchedulingData;
  }
}
