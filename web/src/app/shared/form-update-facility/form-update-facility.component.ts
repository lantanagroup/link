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
import { GlobalApiService } from 'src/services/api/globals/globals-api.service';
import { Events, NormalizationClass, QueryPlans, TenantConceptMap, TenantDetails, Schedule, AuthClass } from '../interfaces/tenant.model';
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
  facilityDetails: TenantDetails | null = null;
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
        copyLocationIdentifierToType: new FormControl(0),
        encounterStatusTransformer: new FormControl(0),
        fixPeriodDates: new FormControl(0),
        fixResourceId: new FormControl(0),
      }),
      afterPatientResourceQuery: new FormGroup({
        patientDataResourceFilter: new FormControl(0)
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
        conceptMaps: this.globalApiService.getContentObservable<TenantConceptMap[]>(`${this.facilityId}/conceptMap`)
      }).subscribe(({ measureDefs, facilityDetails, conceptMaps }) => {
        this.measureDefs = measureDefs
        this.facilityDetails = facilityDetails
        
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
      events: this.mapNormalizationsApiDataToForm(facilityDetails?.events),
      conceptMaps: this.mapConceptMapData(conceptMaps),
      scheduling: {
        queryPatientListCron: facilityDetails?.scheduling?.queryPatientListCron,
        dataRetentionCheckCron: facilityDetails?.scheduling?.dataRetentionCheckCron,
        generateAndSubmitReports: this.mapSchedulingApiDataToForm(facilityDetails?.scheduling?.generateAndSubmitReports)
      },
      fhirQuery: {
        fhirServerBase: facilityDetails?.fhirQuery?.fhirServerBase,
        parallelPatients: facilityDetails?.fhirQuery?.parallelPatients.toString(),
        authClass: facilityDetails?.fhirQuery?.authClass,
        queryPlans: this.mapQueryPlansApiDataToForm(facilityDetails?.fhirQuery?.queryPlans)
      },
      censusAcquisitionMethod: '', // todo : doesn't exist in endpoint
      bulkWaitTimeInMilliseconds: facilityDetails?.bulkWaitTimeInMilliseconds
    })

    // disable Tenant ID field
    this.facilitiesForm.get('id')?.disable()

    this.isDataLoaded = true;
  }

  /// ********************* ///
  /// * MAPPING FUNCTIONS * ///
  /// ********************* ///

  // * Map Scheduling Data

  mapSchedulingApiDataToForm(data: Schedule[] | undefined) {
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

  mapSchedulingFormDataToApi(data: Schedule[] | any): Schedule[] {
    if (!data || !this.measureDefs) {
      return []
    }

    const transformedData = data.map((schedule: Schedule) => {
      const measureIds = schedule.measureIds
        .map((isChecked, index) => isChecked ? this.measureDefs[index].measureId : null)
        .filter((id): id is string => id !== null) as string[]

      return { ...schedule, measureIds }
    })

    return transformedData
  }


  // * Map Concept Maps 

  mapConceptMapData(data: TenantConceptMap[] | FormConceptMap[] | any) {
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

  mapQueryPlansApiDataToForm(data: QueryPlans | undefined) { 
    if(!data) {
      return []
    }
    let queryPlans: any = []
    for (const key of Object.keys(data)) {
      queryPlans = [
        ...queryPlans,
        {
          measureId: key,
          // plan: this.convertJsonToYaml(data[key])
          plan: JSON.stringify(data[key])
        }
      ]
    }
    return queryPlans
  }

  mapQueryPlansFormDataToApi(data: any) {
    if(!data) {
      return []
    }
    const queryPlansArray: any = data.map((obj: any) => {
      const formattedPlan = {
        // [obj.measureId]: this.convertYamlToJson(obj.plan)
        [obj.measureId]: JSON.parse(obj.plan)
      }
      return formattedPlan
    })

    const queryPlansObject = queryPlansArray.reduce((acc: any, obj: any) => {
      const key = Object.keys(obj)[0] // we only have 1 key
      acc[key] = obj[key]
      return acc
    }, {})

    return queryPlansObject
  }


  // Map Normalizations
  mapNormalizationsApiDataToForm(data: Events | undefined) {

    if(!data) {
      return {}
    }

    let afterPatientDataQueryEvents = {}
    let afterPatientResourceQueryEvents = {}
    if(data?.afterPatientDataQuery && data?.afterPatientDataQuery !== null) {
      const thisEvent = data.afterPatientDataQuery
    
      afterPatientDataQueryEvents = {
        codeSystemCleanup: thisEvent.some((item: string) => item === NormalizationClass.codeSystemCleanup) ? 1 : 0,
        containedResourceCleanup: thisEvent.some((item: string) => item === NormalizationClass.containedResourceCleanup) ? 1 : 0,
        copyLocationIdentifierToType: thisEvent.some((item: string) => item === NormalizationClass.copyLocationIdentifierToType) ? 1 : 0,
        encounterStatusTransformer: thisEvent.some((item: string) => item === NormalizationClass.encounterStatusTransformer) ? 1 : 0,
        fixPeriodDates: thisEvent.some((item: string) => item === NormalizationClass.fixPeriodDates) ? 1 : 0,
        fixResourceId: thisEvent.some((item: string) => item === NormalizationClass.fixResourceId) ? 1 : 0
      }
    }

    if(data?.afterPatientResourceQuery && data?.afterPatientResourceQuery !== null) {
      afterPatientDataQueryEvents = {
        patientDataResourceFilter: data.afterPatientResourceQuery.some((item: string) => item === NormalizationClass.patientDataResourceFilter) ? 1 : 0
      }
    }

    const normalizations = {
      afterPatientDataQuery: afterPatientDataQueryEvents,
      afterPatientResourceQuery: afterPatientResourceQueryEvents
    }

    return normalizations
  }

  mapNormalizationsFormDataToApi(data: any) {
    let transformedData: any = {}

    if (!data) {
      return {}
    }

    for(const [key, value] of Object.entries(data)) {
      if(value && typeof value === 'object') {
        const filteredValues = Object.fromEntries(Object.entries(value).filter(([key, subvalue]) => subvalue === 1))

        if(Object.keys(filteredValues).length > 0) {
          const valuesArray = Object.keys(filteredValues)

          transformedData[key] = valuesArray.map(key => NormalizationClass[key as keyof typeof NormalizationClass]).filter(Boolean)
        } else {
          transformedData[key] = null
        }
      }
    }

    return transformedData
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

    // ! add facility id back in
    if(this.facilityId)
      formData.id = this.facilityId

    // grab the concept map submission data and remove
    const conceptMaps: TenantConceptMap[] | null = formData.conceptMaps ? this.mapConceptMapData(formData.conceptMaps) : null;
    delete formData.conceptMaps
    
    // convert form schedules to api scheduels
    if(formData.scheduling?.generateAndSubmitReports) {
      formData.scheduling.generateAndSubmitReports = this.mapSchedulingFormDataToApi(formData.scheduling.generateAndSubmitReports)
    }

    // convert form events to api events
    if(formData.events) {
      formData.events = this.mapNormalizationsFormDataToApi(formData.events)
    }
    
    // convert Yaml to Json for query plans, and set measureIds as keys
    if(formData.fhirQuery?.queryPlans && Object.entries(formData.fhirQuery.queryPlans).length > 0) {
      formData.fhirQuery.queryPlans = this.mapQueryPlansFormDataToApi(formData.fhirQuery.queryPlans)
    }    

    // ! deleting certain data points for now
    delete formData.censusAcquisitionMethod
    delete formData.description
    delete formData.vendor
    // ! remove when these are built/clarified

    if (this.facilitiesForm.valid) {
      try {
        let response;
        this.isDataLoaded = false;
        if (this.facilityId) {
          // Update existing facility
          forkJoin({
            updateTenantResponse: this.globalApiService.putContentObservable(`tenant/${this.facilityId}`, formData),
            // todo : update concept maps
            // updateConceptMapResponse: this.globalApiService.putContentObservable(`${this.facilityId}/conceptMap`, conceptMaps)
          }).subscribe(({ updateTenantResponse }) => {
            this.isDataLoaded = true;
            this.toastService.showToast(
              'Facility Updated',
              `${formData.name} has been successfully updated.`,
              'success'
            )
            this.router.navigate(['/facilities/facility', this.facilityId])
          })
        } else {
          // Create new facility
          forkJoin({
            createTenantResponse: this.globalApiService.postContentObservable('tenant', formData),
            // todo : create concept maps
            // createConceptMapResponse: this.globalApiService.postContentObservable(`${this.facilityId}/conceptMap`, conceptMaps)
          }).subscribe(({ createTenantResponse }) => {
            this.isDataLoaded = true
            this.toastService.showToast(
              'New Tenant Created',
              `${this.facilitiesForm.value.name} has been successfully created.`,
              'success'
            )
            this.router.navigate(['/facilities/'])
          })
        }
      } catch (error: any) {
        console.error('Error submitting facility data', error);
        this.isDataLoaded = true;
        this.toastService.showToast(
          `Facility Error: ${error.error.status}`,
          `Error while adding/updating ${this.facilitiesForm.value.name}: ${error.error.message}`,
          'failed'
        )
      }
    } else {
      this.facilitiesForm.markAllAsTouched()
    }
  }
}
