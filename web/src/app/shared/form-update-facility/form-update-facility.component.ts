import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, FormArray, FormBuilder, Validators, Form } from '@angular/forms';
import { ReactiveFormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import * as yaml from 'js-yaml'
import { SectionHeadingComponent } from '../section-heading/section-heading.component';
import { AccordionComponent } from '../accordion/accordion.component';
import { ButtonComponent } from '../button/button.component';
import { IconComponent } from '../icon/icon.component';
import { ToastComponent } from '../toast/toast.component';
import { ToastService } from 'src/services/toasts/toast.service';
import { Router } from '@angular/router';
import { FacilitiesApiService } from 'src/services/api/facilities/facilities-api.service';
import { GlobalApiService } from 'src/services/api/globals/globals-api.service';
import { Events, NormalizationClass, QueryPlans, TenantConceptMap, TenantDetails, Schedule } from '../interfaces/tenant.model';
import { MeasureDef } from '../interfaces/measures.model';
import { PascalCaseToSpace } from 'src/app/helpers/GlobalPipes.pipe';
import { LoaderComponent } from '../loader/loader.component';

interface FormConceptMap {
  id: string
  name?: string
  contexts: string
  map?: any // todo: not sure what this looks like 
}

@Component({
  selector: 'app-form-update-facility',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SectionHeadingComponent, AccordionComponent, ButtonComponent, IconComponent, ToastComponent, LoaderComponent, PascalCaseToSpace],
  templateUrl: './form-update-facility.component.html',
  styleUrls: ['./form-update-facility.component.scss']
})
export class FormUpdateFacilityComponent {

  @Input() facilityId?: string | null = null;
  measureDefs: MeasureDef[] = [];
  isMeasureDefsLoaded: boolean = false;
  isDataLoaded: boolean = false;

  constructor(
    private fb: FormBuilder, 
    private globalApiService: GlobalApiService,
    private toastService: ToastService, 
    private router: Router
  ) { }

  facilitiesForm = new FormGroup ({
    id: new FormControl('', [Validators.required]),
    name: new FormControl('', [Validators.required]),
    description: new FormControl(''),
    bundling: new FormGroup ({
      name: new FormControl('', [Validators.required])
    }),
    cdcOrgId: new FormControl(''),
    connectionString: new FormControl(''),
    vendor: new FormControl(''),
    retentionPeriod: new FormControl(''),
    events: new FormGroup ({
      afterPatientDataQuery: new FormGroup({
        codeSystemCleanup: new FormControl(0),
        containedResourceCleanup: new FormControl(0),
        copyLocation: new FormControl(0),
        encounterStatus: new FormControl(0),
        fixPeriodDates: new FormControl(0),
        fixResourceIds: new FormControl(0),
      }),
      afterPatientResourceQuery: new FormGroup({
        patientDataResource: new FormControl(0)
      })
    }),
    conceptMaps: this.fb.array([]),
    scheduling: new FormGroup({
      queryPatientListCron: new FormControl(''),
      dataRetentionCheckCron: new FormControl(''),
      generateAndSubmitReports: this.fb.array([], [Validators.required])
    }),
    fhirQuery: new FormGroup({
      fhirServerBase: new FormControl(''),
      parallelPatients: new FormControl(''),
      authClass: new FormControl(''),
      queryPlans: this.fb.array([])
    }),
    censusAcquisitionMethod: new FormControl(''),
    bulkWaitTimeInMilliseconds: new FormControl('')
  })

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
    return this.facilitiesForm.get('scheduling.generateAndSubmitReports') as FormArray;
  }

  createSchedule(): FormGroup {
    return this.fb.group({
      measureIds: this.fb.array([]),
      reportingPeriodMethod: new FormControl('LastMonth'),
      cron: new FormControl(''),
      regenerateIfExists: new FormControl(1)
    });
  }

  addSchedule() {
    this.schedules.push(this.createSchedule())
    this.addMeasureIdControls(this.schedules.length - 1)
  }

  addMeasureIdControls(scheduleIndex: number) {
    const measureIdsArray = (this.schedules.at(scheduleIndex) as FormGroup).get('measureIds') as FormArray
    this.measureDefs.forEach(() => measureIdsArray.push(new FormControl(false)))
  }

  getMeasureIds(index: number): FormArray {
    const scheduleFormGroup = this.schedules.at(index) as FormGroup
    return scheduleFormGroup.get('measureIds') as FormArray
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

  // to correctly create checkboxes when facility data is present
  initializeCheckboxStates(data: string[]) {
    return this.measureDefs.map(def => data.includes(def.measureId))
  }

  loadScheduleDataFromApi(data: Schedule) {
    const checkboxStates = this.initializeCheckboxStates(data.measureIds)
    this.setCheckboxStatesForSchedule(checkboxStates, 0)
  }

  setCheckboxStatesForSchedule(checkboxStates: boolean[], index: number) {
    const measureIdsArray = (this.schedules.at(index) as FormGroup).get('measureIds') as FormArray

    measureIdsArray.clear()

    checkboxStates.forEach(state => measureIdsArray.push(new FormControl(state)))
  }

  // Query Plan Dynamic Fields
  get queryPlans(): FormArray {
    return this.facilitiesForm.get('fhirQuery.queryPlans') as FormArray;
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
      forkJoin({
        measureDefs: this.globalApiService.getContentObservable<MeasureDef[]>('measureDef'),
        facilityDetails: this.globalApiService.getContentObservable<TenantDetails>(`tenant/${this.facilityId}`),
        conceptMaps: this.globalApiService.getContentObservable(`${this.facilityId}/conceptMap`)
      }).subscribe(({ measureDefs, facilityDetails, conceptMaps }) => {
        this.measureDefs = measureDefs
        
        if(this.facilityId) 
          this.setInitialValues(facilityDetails, conceptMaps)

        this.isMeasureDefsLoaded = true
        this.isDataLoaded = true
      })
    } else {
      this.isDataLoaded = true
      this.globalApiService.getContentObservable<MeasureDef[]>('measureDef').subscribe({
        next: (measureDefs) => {
          this.measureDefs = measureDefs
        },
        error: (error) => {
          console.error('Error fetching measureDefs:', error)
        },
        complete: () => {
          this.isMeasureDefsLoaded = true
        }
      })
    }
  }

  private async setInitialValues(facilityDetails: TenantDetails, conceptMaps: any) {

    console.log('facility details:', facilityDetails)
    console.log('Concept Maps:', conceptMaps)
    console.log('Measure Defs', this.measureDefs)

    // Schedules
    if (facilityDetails?.scheduling?.generateAndSubmitReports) {
      for (const schedule of facilityDetails.scheduling.generateAndSubmitReports) {
        this.addSchedule();
      }
    }

    // Concept Maps
    if (conceptMaps) {
      for (const cm of conceptMaps) {
        this.addConceptMap();
      }
    }

    // Query Plans
    if (facilityDetails?.fhirQuery?.queryPlans) {
      const queryPlans = facilityDetails.fhirQuery.queryPlans
      let count = 0;
      if(typeof queryPlans === 'object' && queryPlans !== null && !Array.isArray(queryPlans)) {
        count = Object.keys(queryPlans).length
      } else {
        count = queryPlans.length
      }
      for (let i = 0; i < count; i++) {
        this.addQueryPlan();
      }
    }

    this.facilitiesForm.patchValue({

      id: this.facilityId,
      name: facilityDetails?.name,
      description: null, // todo : doesn't exist in endpoint
      bundling: {
        name: facilityDetails?.bundling?.name
      },
      cdcOrgId: facilityDetails?.cdcOrgId,
      connectionString: facilityDetails?.connectionString,
      vendor: null, // todo : doesn't exist in endpoint
      retentionPeriod: facilityDetails?.retentionPeriod,
      events: this.mapNormalizationsToFormData(facilityDetails?.events),
      conceptMaps: this.mapConceptMapData(conceptMaps),
      scheduling: {
        queryPatientListCron: facilityDetails?.scheduling?.queryPatientListCron,
        dataRetentionCheckCron: facilityDetails?.scheduling?.dataRetentionCheckCron,
        generateAndSubmitReports: this.mapSchedulingData(facilityDetails?.scheduling?.generateAndSubmitReports)
      },
      fhirQuery: {
        fhirServerBase: facilityDetails?.fhirQuery?.fhirServerBase,
        parallelPatients: facilityDetails?.fhirQuery?.parallelPatients.toString(),
        authClass: facilityDetails?.fhirQuery?.authClass,
        queryPlans: this.mapQueryPlansApiDataToFormData(facilityDetails?.fhirQuery?.queryPlans)
      },
      censusAcquisitionMethod: '', // todo : doesn't exist in endpoint
      bulkWaitTimeInMilliseconds: facilityDetails?.bulkWaitTimeInMilliseconds
    })

    this.isDataLoaded = true;
  }

  /// ********************* ///
  /// * MAPPING FUNCTIONS * ///
  /// ********************* ///

  // * Map Scheduling Data

  mapSchedulingData(data: Schedule[] | undefined) {
    if (!data || !this.measureDefs) {
      return []
    }

    const generateAndSubmitReports = data.map((schedule: Schedule) => {
      return {
        ...schedule,
        measureIds: this.loadScheduleDataFromApi(schedule)
      }
    })
    return generateAndSubmitReports;
  }


  // * Map Concept Maps 

  mapConceptMapData(data: TenantConceptMap[] | FormConceptMap[]) {
    if(!data) {
      return []
    }
    const conceptMaps = data.map((cm: TenantConceptMap | FormConceptMap) => {
      return {
        id: cm?.id,
        name: cm?.name,
        contexts: this.convertLineBreaks(cm?.contexts),
        map: cm?.map
      }
    })
    return conceptMaps
  }

  // * Map Query Plans

  mapQueryPlansApiDataToFormData(data: QueryPlans | undefined) { 
    if(!data) {
      return []
    }
    let queryPlans: any = []
    for (const key of Object.keys(data)) {
      queryPlans = [
        ...queryPlans,
        {
          measureId: key,
          plan: this.convertJsonToYaml(data[key])
        }
      ]
    }
    return queryPlans
  }


  // Map Normalizations
  mapNormalizationsToFormData(data: Events | undefined) {

    if(!data) {
      return {}
    }

    let afterPatientDataQueryEvents = {}
    let afterPatientResourceQueryEvents = {}
    if(data?.afterPatientDataQuery !== null) {
      const thisEvent = data.afterPatientDataQuery
    
      afterPatientDataQueryEvents = {
        codeSystemCleanup: thisEvent.some((item: string) => item === NormalizationClass.CodeSystemCleanup) ? 1 : 0,
        containedResourceCleanup: thisEvent.some((item: string) => item === NormalizationClass.ContainedResourceCleanup) ? 1 : 0,
        copyLocation: thisEvent.some((item: string) => item === NormalizationClass.CopyLocationIdentifierToType) ? 1 : 0,
        encounterStatus: thisEvent.some((item: string) => item === NormalizationClass.EncounterStatusTransformer) ? 1 : 0,
        fixPeriodDates: thisEvent.some((item: string) => item === NormalizationClass.FixPeriodDates) ? 1 : 0,
        fixResourceIds: thisEvent.some((item: string) => item === NormalizationClass.FixResourceId) ? 1 : 0
      }
    }

    if(data?.afterPatientResourceQuery !== null) {
      afterPatientDataQueryEvents = {
        patientDataResource: data.afterPatientResourceQuery.some((item: string) => item === NormalizationClass.PatientDataResourceFilter) ? 1 : 0
      }
    }

    const normalizations = {
      afterPatientDataQuery: afterPatientDataQueryEvents,
      afterPatientResourceQuery: afterPatientResourceQueryEvents
    }

    return normalizations
  }

  /// ******************** ///
  /// * Helper Functions * ///
  /// ******************** ///

  // Function to convert line breaks to array and back
  convertLineBreaks(data: string | string[]) {
    if( typeof data === 'string' )
      return data.split('\n')
    else {
      return data.join('\n')
    }
  }

  // Functions to convert JSON to YAML and back
  convertJsonToYaml(jsonData: any): string {
    try {
      return yaml.dump(jsonData)
    } catch(error) {
      console.error('Error converting JSON to YAML', error)
      return ''
    }
  }

  convertYamlToJson(yamlData: any) {
    try {
        const jsonObject = yaml.load(yamlData)
        return JSON.stringify(jsonObject, null, 2) // The second and third arguments beautify the output
    } catch (error) {
        console.error('Error converting YAML to JSON', error)
        return null
    }
  }


  /// *************** ///
  /// * Form Submit * ///
  /// *************** ///

  async onSubmit() {
    const formData = this.facilitiesForm.value;
    console.log('formData:', formData)
    // todo : separate out concept map data
    if(this.facilitiesForm.value?.conceptMaps) {
      // todo : fix typescript error
      const conceptMapData = this.mapConceptMapData([])
    }

    const tenantData = {

    };

    if (this.facilitiesForm.valid) {
      try {
        let response;
        if (this.facilityId) {
          // Update existing facility
          // response = await this.facilitiesApiService.updateFacility(this.facilityId, submissionData);
          // response = await this.facilitiesApiService.updateFacility(this.facilityId, this.facilitiesForm.value);

          this.toastService.showToast(
            'Facility Updated',
            `${this.facilitiesForm.value.name} has been successfully updated.`,
            'success'
          )
          // this.router.navigate(['/facilities/facility', this.facilityId])
        } else {
          // Create new facility
          // response = await this.facilitiesApiService.createFacility(submissionData);
          // response = await this.facilitiesApiService.createFacility(this.facilitiesForm.value);

          this.toastService.showToast(
            'Facility Created',
            `${this.facilitiesForm.value.name} has been successfully created.`,
            'success'
          )
          // this.router.navigate(['/facilities/'])
        }
      } catch (error) {
        console.error('Error submitting facility data', error);
        this.toastService.showToast(
          'Facility Error',
          `Error while adding/updating ${this.facilitiesForm.value.name}.`,
          'failed'
        )
      }
      // alert(JSON.stringify(this.facilitiesForm.value))
    } else {
      this.facilitiesForm.markAllAsTouched()
    }
  }
}
