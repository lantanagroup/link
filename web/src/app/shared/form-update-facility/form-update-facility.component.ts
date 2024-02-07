import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormControl, FormGroup, FormArray, FormBuilder, Validators, Form, AbstractControl } from '@angular/forms';
import { ReactiveFormsModule } from '@angular/forms';
import { forkJoin, from, switchMap, map } from 'rxjs';
import * as yaml from 'js-yaml'
import { SectionHeadingComponent } from '../section-heading/section-heading.component';
import { AccordionComponent } from '../accordion/accordion.component';
import { ButtonComponent } from '../button/button.component';
import { IconComponent } from '../icon/icon.component';
import { ToastComponent } from '../toast/toast.component';
import { LoaderComponent } from '../loader/loader.component';
import { ToastService } from 'src/services/toasts/toast.service';
import { GlobalApiService } from 'src/services/api/globals/globals-api.service';
import { Events, NormalizationClass, QueryPlans, TenantConceptMap, TenantDetails, Schedule, AuthClass } from '../interfaces/tenant.model';
import { MeasureDef } from '../interfaces/measures.model';
import { PascalCaseToSpace } from 'src/app/helpers/GlobalPipes.pipe';
import { urlValidator, iso8601Validator, databaseValidator } from 'src/app/helpers/FormValidators';

/// ********************* ///
/// * TABLE OF CONTENTS * ///
/// ********************* ///
// 1. Interfaces
// 2. Component variables and constructors
// 3. Define the form
// 4. Functions to handle dynamic fields (ie. repeaters)
//    a. Concept Maps
//    b. Schedules
//    c. Query Lists
//    d. Query Plans
// 5. Mapping Functions/Middleware
//    a. Concept Maps
//    b. Schedules
//    c. Query Plans
//    d. Normalizations
//    e. Auth Classes
// 6. Helper Functions
//    a. Convert line breaks to array and back
//    b. Convert JSON to YAML and back
// 7. View Initialization
//    a. ngOnInit()
//        1. Conditionals
//        2. API calls
//    a. Set Initial Values (if Edit)
//        1. Generate repeater fields
//        2. Populate form values
// 8. Form Submit
//    a. Transform form data for API requests
//    b. Send API data
//        1. Update existing facility
//        2. Create new facility


/// ***************** ///
/// * 1. Interfaces * ///
/// ***************** ///

interface FormConceptMap {
  id: string
  name?: string
  contexts: string
  conceptMap?: string // convert from JSON
}

interface FormConditionals {
  [key: string]: string | null | boolean
}

@Component({
  selector: 'app-form-update-facility',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SectionHeadingComponent, AccordionComponent, ButtonComponent, IconComponent, ToastComponent, LoaderComponent, PascalCaseToSpace],
  templateUrl: './form-update-facility.component.html',
  styleUrls: ['./form-update-facility.component.scss']
})
export class FormUpdateFacilityComponent {
  /// ******************************************* ///
  /// * 2. Component variables and constructors * ///
  /// ******************************************* ///

  @Input() facilityId?: string | null = null;
  facilityDetails: TenantDetails | null = null;
  measureDefs: MeasureDef[] = [];
  isMeasureDefsLoaded: boolean = false;
  isDataLoaded: boolean = false;

  conditionals: FormConditionals = {
    censusAcquisition: null,
    authClass: null
  }

  constructor(
    private fb: FormBuilder, 
    private globalApiService: GlobalApiService,
    private toastService: ToastService, 
    private router: Router
  ) { }

  /// ********************** ///
  /// * 3. Define the Form * ///
  /// ********************** ///

  facilitiesForm = new FormGroup ({
    id: new FormControl('', [Validators.required]),
    name: new FormControl('', [Validators.required]),
    description: new FormControl(''),
    bundling: new FormGroup ({
      name: new FormControl('', [Validators.required])
    }),
    cdcOrgId: new FormControl(''), // todo : validation for 5 char min?
    connectionString: new FormControl('', [databaseValidator()]),
    vendor: new FormControl(''),
    retentionPeriod: new FormControl('', [iso8601Validator()]), // todo : confirm this is the correct regex
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
    queryList: new FormGroup ({
      fhirServerBase: new FormControl('', [urlValidator()]),
      lists: this.fb.array([])
    }),
    conceptMaps: this.fb.array([]),
    scheduling: new FormGroup({
      queryPatientListCron: new FormControl(''),
      dataRetentionCheckCron: new FormControl(''),
      generateAndSubmitReports: this.fb.array([])
    }),
    fhirQuery: new FormGroup({
      fhirServerBase: new FormControl('', [urlValidator()]),
      parallelPatients: new FormControl(''),
      authClass: new FormControl(''),
      queryPlans: this.fb.array([]),
      basicAuth: new FormGroup({
        username: new FormControl(''),
        password: new FormControl('')
      }),
      basicAuthAndApiKey: new FormGroup({
        username: new FormControl(''),
        password: new FormControl(''),
        apiKey: new FormControl('')
      }),
      tokenAuth: new FormGroup({
        token: new FormControl('')
      }),
      azureAuth: new FormGroup({
        tokenUrl: new FormControl(''),
        clientId: new FormControl(''),
        secret: new FormControl(''),
        resource: new FormControl('')
      }),
      epicAuth: new FormGroup({
        key: new FormControl(''),
        tokenUrl: new FormControl(''),
        clientId: new FormControl(''),
        audience: new FormControl('')
      }),
      cernerAuth: new FormGroup({
        tokenUrl: new FormControl(''),
        clientId: new FormControl(''),
        secret: new FormControl(''),
        scopes: new FormControl('')
      })
    }),
    censusAcquisitionMethod: new FormControl(''),
    bulkWaitTimeInMilliseconds: new FormControl('')
  })

  /// ******************************************************** ///
  /// * 4. Functions to handle dynamic fiels (ie. repeaters) * ///
  /// ******************************************************** ///

  // a. Concept Maps
  get conceptMaps(): FormArray {
    return this.facilitiesForm.get('conceptMaps') as FormArray;
  }

  createConceptMap(): FormGroup {
    return this.fb.group({
      id: new FormControl(''),
      name: new FormControl(''),
      contexts: new FormControl(''),
      conceptMap: new FormControl('')
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

  // b. Schedules
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

  // c. Query Lists
  get lists(): FormArray {
    return this.facilitiesForm.get('queryList.lists') as FormArray
  }

  createList(): FormGroup {
    return this.fb.group({
      measureId: new FormControl(''),
      listId: new FormControl('')
    })
  }

  addList() {
    this.lists.push(this.createList())
  }

  getAddListHandler(): () => void {
    return () => this.addList()
  }

  removeList(index: number) {
    this.lists.removeAt(index)
  }

  getRemoveListHandler(index: number): () => void {
    return () => this.removeList(index)
  }

  // d. Query Plans
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

  /// *********************************** ///
  /// * 5. Mapping Functions/Middleware * ///
  /// *********************************** ///

  // a. Concept Maps 
  mapConceptMapData(data: TenantConceptMap[] | FormConceptMap[] | any) {
    if(!data) {
      return []
    }
    const conceptMaps = data.map((cm: TenantConceptMap | FormConceptMap) => {
      // if conceptMap is object, convert to string for form, otherwise parse JSON back to object for api submission
      let conceptMapData = null
      if(cm?.conceptMap) {
        conceptMapData = typeof cm.conceptMap === 'object' ? JSON.stringify(cm.conceptMap) : JSON.parse(cm.conceptMap)
      }

      return {
        id: cm?.id,
        name: cm?.name,
        contexts: this.convertLineBreaks(cm?.contexts),
        conceptMap: conceptMapData
      }
    })
    return conceptMaps
  }

  // b. Schedules
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

  // c. Query Plans
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

  // d. Auth Classes
  authClassFormDataToApi(data: any): any {
    // get the value of the authClass
    const authValue = data.authClass
    
    let allAuthTypes = [
      'basicAuth',
      'basicAuthAndApiKey',
      'tokenAuth',
      'azureAuth',
      'epicAuth',
      'cernerAuth'
    ]

    if(authValue) {
      // take the value and remove it from the allAuthTypes array
      allAuthTypes = allAuthTypes.filter(item => item !== authValue.charAt(0).toLowerCase() + authValue.slice(1))

      // set the value of the authClass to the enum
      data.authClass = AuthClass[authValue as keyof typeof AuthClass]
    }
    
    // loop through the array and set all the values to null
    allAuthTypes.forEach(authType => {
      data[authType] = null
    })
    
    // return the data group
    return data
  }

  // e. Normalizations
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
      afterPatientResourceQueryEvents = {
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

  /// *********************** ///
  /// * 6. Helper Functions * ///
  /// *********************** ///

  // a. Convert line breaks to array and back
  convertLineBreaks(data: string | string[]) {
    if( typeof data === 'string' )
      return data.split('\n')
    else {
      return data.join('\n')
    }
  }

  // b. Convert JSON to YAML and back
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

  /// ************************** ///
  /// * 7. View Initialization * ///
  /// ************************** ///

  // a. ngOnInit()
  async ngOnInit(): Promise<void> {


    // 1. Conditionals
    this.facilitiesForm.get('fhirQuery.authClass')?.valueChanges.subscribe(value => {
      this.conditionals['authClass'] = value
    })

    this.facilitiesForm.get('censusAcquisitionMethod')?.valueChanges.subscribe(value => {
      this.conditionals['censusAcquisitionMethod'] = value
    })

    
    // 2. API Calls
    // load up the facility data if passed into the url
    if (this.facilityId) {
      forkJoin({ // call initial endpoints for measureDefs, tenant Details and concept maps
        measureDefs: this.globalApiService.getContentObservable<MeasureDef[]>('measureDef'),
        facilityDetails: this.globalApiService.getContentObservable<TenantDetails>(`tenant/${this.facilityId}`),
        conceptMaps: this.globalApiService.getContentObservable<TenantConceptMap[]>(`${this.facilityId}/conceptMap`)
      }).pipe(
        // conceptMap endpoint doesn't actually return the conceptMap part
        switchMap(({ measureDefs, facilityDetails, conceptMaps }) => {
          // Process conceptMaps
          const conceptMapRequests = conceptMaps.map(conceptMap =>
            this.globalApiService.getContentObservable(`${this.facilityId}/conceptMap/${conceptMap.id}`)
              .pipe(
                map(conceptMapDetails => {
                  // Since this includes the id, name and contexts, we can replace it with this
                  return conceptMapDetails
                })
              )
            )
      
          // Wait for all additional conceptMap details requests to complete
          return forkJoin(conceptMapRequests).pipe(
            map(updatedConceptMaps => ({
              measureDefs,
              facilityDetails,
              conceptMaps: updatedConceptMaps
            }))
          )
        })
      ).subscribe(({ measureDefs, facilityDetails, conceptMaps }) => {
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

  // b. Set Initial Values (if Edit)
  private async setInitialValues(facilityDetails: TenantDetails, conceptMaps: any) {

    // 1. Generate repeater fields
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

    // Query Lists
    if (facilityDetails?.queryList?.lists) {
      for (const list of facilityDetails.queryList.lists) {
        this.addList();
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

    // 2. Populate form values
    this.facilitiesForm.patchValue({

      id: this.facilityId,
      name: facilityDetails?.name,
      description: null, // todo : doesn't exist in endpoint
      bundling: {
        name: facilityDetails?.bundling?.name
      },
      cdcOrgId: facilityDetails?.cdcOrgId,
      connectionString: facilityDetails?.connectionString,
      vendor: facilityDetails?.vendor,
      retentionPeriod: facilityDetails?.retentionPeriod,
      events: this.mapNormalizationsApiDataToForm(facilityDetails?.events),
      conceptMaps: this.mapConceptMapData(conceptMaps),
      scheduling: {
        queryPatientListCron: facilityDetails?.scheduling?.queryPatientListCron,
        dataRetentionCheckCron: facilityDetails?.scheduling?.dataRetentionCheckCron,
        generateAndSubmitReports: this.mapSchedulingApiDataToForm(facilityDetails?.scheduling?.generateAndSubmitReports)
      },
      queryList: {
        fhirServerBase: facilityDetails?.queryList?.fhirServerBase,
        lists: facilityDetails?.queryList?.lists
      },
      fhirQuery: {
        fhirServerBase: facilityDetails?.fhirQuery?.fhirServerBase,
        parallelPatients: facilityDetails?.fhirQuery?.parallelPatients.toString(),
        authClass: facilityDetails?.fhirQuery?.authClass ? facilityDetails.fhirQuery.authClass.split('.').pop() : null,
        queryPlans: this.mapQueryPlansApiDataToForm(facilityDetails?.fhirQuery?.queryPlans),
        basicAuth: facilityDetails?.fhirQuery?.basicAuth,
        basicAuthAndApiKey: facilityDetails?.fhirQuery?.basicAuthAndApiKey,
        tokenAuth: facilityDetails?.fhirQuery?.tokenAuth,
        azureAuth: facilityDetails?.fhirQuery?.azureAuth,
        epicAuth: facilityDetails?.fhirQuery?.epicAuth,
        cernerAuth: facilityDetails?.fhirQuery?.cernerAuth
      },
      censusAcquisitionMethod: '', // todo : doesn't exist in endpoint
      bulkWaitTimeInMilliseconds: facilityDetails?.bulkWaitTimeInMilliseconds
    })

    // disable Tenant ID field
    this.facilitiesForm.get('id')?.disable()

    this.isDataLoaded = true;
  }

  /// ****************** ///
  /// * 8. Form Submit * ///
  /// ****************** ///

  async onSubmit() {
    const formData = this.facilitiesForm.value;

    // a. Transform form data for API requests
    // ! Delete the censusAcquisitionMethod because it's only there to monitor a conditional, this value doesn't get sent to API
    delete formData.censusAcquisitionMethod

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

    // set the authClass to the correct enum and null out the specific auth keys
    if(formData.fhirQuery) {
      formData.fhirQuery = this.authClassFormDataToApi(formData.fhirQuery)
    }
    
    // convert Yaml to Json for query plans, and set measureIds as keys
    if(formData.fhirQuery?.queryPlans && Object.entries(formData.fhirQuery.queryPlans).length > 0) {
      formData.fhirQuery.queryPlans = this.mapQueryPlansFormDataToApi(formData.fhirQuery.queryPlans)
    } else {
      delete formData.fhirQuery?.queryPlans
    }

    // ! deleting certain data points for now
    delete formData.description
    delete formData.vendor
    // ! remove when these are built/clarified

    // b. Send API data
    if (this.facilitiesForm.valid) {

      let allObservables = []

      if(conceptMaps) {
        conceptMaps.forEach(conceptMap => {
          allObservables.push(this.globalApiService.putContentObservable(`${this.facilityId}/conceptMap`, conceptMap))
        })
      }

      try {
        this.isDataLoaded = false;
        if (this.facilityId) {
          // 1. Update existing facility
          allObservables.unshift(this.globalApiService.putContentObservable(`tenant/${this.facilityId}`, formData))

          forkJoin(allObservables).subscribe({
            next: (responses) => {
              let tenantResponse = responses[0],
                  conceptMapResponses = responses.slice(1)
              console.log(responses.length + '/' + allObservables.length, 'complete')
            },
            error: (error) => {
              this.isDataLoaded = true;
              console.error('Error fetching measureDefs:', error)
            },
            complete: () => {
              this.isDataLoaded = true;
              this.toastService.showToast(
                'Facility Updated',
                `${formData.name} has been successfully updated.`,
                'success'
              )

              this.router.navigate(['/facilities/facility', this.facilityId])
            }
          })
        } else {
          // 2. Create new facility
          allObservables.unshift(this.globalApiService.postContentObservable('tenant', formData))
          forkJoin(allObservables).subscribe(responses => {
            let tenantResponse = responses[0],
                conceptMapResponses = responses.slice(1)

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
