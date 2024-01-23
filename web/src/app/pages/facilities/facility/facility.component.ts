import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { forkJoin, from } from 'rxjs';
import { HeroComponent } from 'src/app/shared/hero/hero.component';
import { SectionComponent } from 'src/app/shared/section/section.component';
import { SectionHeadingComponent } from 'src/app/shared/section-heading/section-heading.component';
import { ButtonComponent } from 'src/app/shared/button/button.component';
import { IconComponent } from 'src/app/shared/icon/icon.component';
import { FacilitiesApiService } from 'src/services/api/facilities/facilities-api.service';
import { GlobalApiService } from 'src/services/api/globals/globals-api.service';
import { CardComponent } from "../../../shared/card/card.component";
import { TabComponent } from 'src/app/shared/tab/tab.component';
import { TabContainerComponent } from 'src/app/shared/tab-container/tab-container.component';
import { LinkComponent } from 'src/app/shared/link/link.component';
import { AccordionComponent } from 'src/app/shared/accordion/accordion.component';
import { TableComponent } from "../../../shared/table/table.component";
import { MiniContentComponent } from 'src/app/shared/mini-content/mini-content.component';
import { Report } from 'src/app/shared/interfaces/report.model';
import { ReportApiService } from 'src/services/api/report/report-api.service';
import { calculatePeriodLength, formatDate } from 'src/app/helpers/ReportHelper';
import { TenantConceptMap, Normalization, QueryPlan, QueryPlans, TenantDetails } from 'src/app/shared/interfaces/tenant.model';
import { PascalCaseToSpace, ConvertDateString } from 'src/app/helpers/GlobalPipes.pipe';
import { LoaderComponent } from 'src/app/shared/loader/loader.component';

@Component({
    selector: 'app-facility',
    standalone: true,
    templateUrl: './facility.component.html',
    styleUrls: ['./facility.component.scss'],
    imports: [CommonModule, HeroComponent, SectionComponent, SectionHeadingComponent, ButtonComponent, IconComponent, CardComponent, TabComponent, TabContainerComponent, LinkComponent, AccordionComponent, TableComponent, MiniContentComponent, LoaderComponent, PascalCaseToSpace]
})
export class FacilityComponent {
  facilityId: string | null = null
  facilityDetails: any = null;
  facilityNormalizations: Normalization[] = []
  dataQueryKeys: string[] = [
    'CodeSystemCleanup',
    'ContainedResouceCleanup',
    'CopyLocationToIdentifierType',
    'EncounterStatusTransformer',
    'FixPeriodDates',
    'FixResourceId'
  ]
  resourceQueryKeys: string[] = [
    'PatientDataResourceFilter'
  ]
  facilityConceptMaps: TenantConceptMap[] = []
  facilityQueryPlans: QueryPlan[] = []
  dtOptions: DataTables.Settings = {};
  isDataLoaded: boolean = false;

  private pascalCaseToSpace = new PascalCaseToSpace
  private convertDateString = new ConvertDateString

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private facilitiesApiService: FacilitiesApiService,
    private globalApiService: GlobalApiService,
    private reportsApiService: ReportApiService
  ) { }

  async ngOnInit() {
    this.route.paramMap.subscribe(params => {
      this.facilityId = params.get('id')

      if (this.facilityId) {
        this.dtOptions = this.calculateDtOptions(this.facilityId)

        forkJoin({
          tenantDetails: this.globalApiService.getContentObservable<TenantDetails>(`tenant/${this.facilityId}`),
          conceptMaps: this.globalApiService.getContentObservable<TenantConceptMap[]>(`${this.facilityId}/conceptMap`)
        }).subscribe(({ tenantDetails, conceptMaps }) => {
          this.facilityDetails = tenantDetails
          this.facilityConceptMaps = conceptMaps

          // get normalizations
          this.facilityNormalizations = [
            ...this.generateNormalizations(this.facilityDetails.events.afterPatientDataQuery, this.dataQueryKeys),
            ...this.generateNormalizations(this.facilityDetails.events.afterPatientResourceQuery, this.resourceQueryKeys)
          ]

          // get query plans
          this.facilityQueryPlans = this.generateQueryPlans(this.facilityDetails.fhirQuery?.queryPlans)

          this.isDataLoaded = true
        })
      } else {
        this.router.navigate(['/facilities'])
      }
    })
  }

  async GetConceptMaps(id: string) {
    try {
      const conceptMaps = await this.globalApiService.getContent(`${id}/conceptMap`)
      this.facilityConceptMaps = conceptMaps
    } catch (error) {
      console.error('Error loading concept maps:', error)
    }
  }

  async GetFacilityDetails(id: string) {
    try {
      const tenantDetail = await this.facilitiesApiService.fetchFacilityById(id);
      this.facilityDetails = tenantDetail;

      // get normalizations
      // todo - fix
      this.facilityNormalizations = [
        ...this.generateNormalizations(tenantDetail.events.afterPatientDataQuery, this.dataQueryKeys),
        ...this.generateNormalizations(tenantDetail.events.afterPatientResourceQuery, this.resourceQueryKeys)
      ]

      // get query plans
      this.facilityQueryPlans = this.generateQueryPlans(tenantDetail.fhirQuery?.queryPlans)

    } catch (error) {
      console.error('Error Loading table data.', error);
    }
  }

  generateEditLink(): { url: string } {
    const url = this.facilityId
      ? `/facilities/edit-facility/${this.facilityId}`
      : '/facilities/add-facility';

    return { url: url };
  }

  generateMeasureChip(measure: string | undefined): string {
    if(measure) {
      const prettyMeasure = this.pascalCaseToSpace.transform(measure)
      return prettyMeasure.split(' ')[0]
    }
    return ''
  }

  generateMeasureName(measure: string | undefined): string {
    if(measure) {
      const prettyMeasure = this.pascalCaseToSpace.transform(measure)
      return prettyMeasure.split(' ').slice(1).join(' ')
    }
    return ''
  }

  generateNormalizations(data: string[], keys: string[]): Normalization[] {
    const normalizations = keys.map(key => {
      const found = data !== null ? data.some(item => item.includes(key)) : false
      return { name: key, value: found ? 'Yes' : 'No'}
    })

    return normalizations
  }

  generateQueryPlans(data: QueryPlans): QueryPlan[] {
    let templateQueryPlans: QueryPlan[] = []
    console.log('data:', data)
    for (const key of Object.keys(data)) {
      templateQueryPlans = [...templateQueryPlans, {
        ...data[key],
        measureId: key
      }]
    }

    return templateQueryPlans
  } 

  calculateDtOptions(facilityId: string): DataTables.Settings {
    // DataTable configuration
    // ! for column ordering, will change
    const columnIdMap = ['TIMESTAMP', 'ACTIVITY', 'DETAILS', 'FACILITY', 'NHSN_ORG_ID', 'REPORTING_PERIOD', 'MEASURES'],
          pageLength = 15

    return {
      serverSide: true,
      processing: true,
      ajax: (dataTablesParameters: any, callback) => {
        const page = Math.ceil(dataTablesParameters.start / pageLength) + 1

        let order = dataTablesParameters.order[0],
            orderBy = columnIdMap[order.column],
            sortAscend = order.dir === 'asc'

        let searchValue = dataTablesParameters.search.value

        const apiFilters = {
          tenantId: facilityId,
          page: page,
          count: pageLength
        }
        // todo : fill in rest of query when built

        from(this.reportsApiService.fetchAllReports(apiFilters))
          .subscribe(response => {
            callback({
              recordsTotal: response?.length,
              recordsFiltered: pageLength,
              data: this.processDataForTable(response)
            })
          })
      },
      lengthChange: false,
      info: false,
      searching: false,
      scrollX: true,
      stripeClasses: ['zebra zebra--even', 'zebra zebra--odd'],
      columns: [{
        title: 'Timestamp',
        data: columnIdMap[0],
        orderable: false,
        createdCell: (cell, cellData) => {
          $(cell).addClass('timestamp');
        }
      },
      {
        title: 'Activity',
        data: columnIdMap[1],
        orderable: false,
        width: '75px',
        createdCell: (cell, cellData) => {
          if (cellData.toLowerCase().includes('initiated')) {
            $(cell).addClass('cell--initiated');
          } else {
            $(cell).addClass('cell--complete');
          }
        },
        render: function (data, type, row) {
          let dotClass;
          switch (row.STATUS) {
            case 'Submitted': 
              dotClass = 'success'
              break
            case 'Draft':
              dotClass = 'neutral'
              break
            default:
              dotClass = 'failed'
          }
          
          return `<div class="d-flex align-items-center">
                  <span class="dot dot--${dotClass}"></span>
                  <span>${data}</span>
                </div>`;
        }
      },
      {
        title: 'Details',
        data: columnIdMap[2],
        orderable: false,
        createdCell: (cell, cellData) => {
          if (cellData.toLowerCase().includes('progress')) {
            $(cell).addClass('cell--initiated cell--inProgress');
          } else {
            $(cell).addClass('cell--complete');
          }
        },
        render: function (data, type, row) {
          if(row.STATUS === 'Submitted') {
            return `<a href="/activities/bundle/${row.FACILITY.id}/${row.ID}">${data}</a>`
          }
          return data
        }
      },
      {
        title: 'Facility',
        data: columnIdMap[3],
        orderable: false,
        width: '175px',
        render: function (data, type, row) {
          return `<a href="/facilities/facility/${data.id}">${data.name}</a>`;
        }
      },
      {
        title: 'NHSN Org Id',
        data: columnIdMap[4],
        orderable: false,
        width: '144px'
      },
      {
        title: 'Reporting Period',
        data: columnIdMap[5],
        orderable: false,
        width: '196px',
        createdCell: (cell, cellData) => {
          if (cellData.toString().toLowerCase() === 'pending') {
            $(cell).addClass('cell--initiated');
          }
        },
        render: function (data, type, row) {
          if (Array.isArray(data)) {
            return `${data[1]}<br>${data[0]}`;
          }
          return data; // 'pending' or 'n/a'
        }
      },
      {
        title: 'Measures',
        data: columnIdMap[6],
        orderable: false,
        render: function (data, type, row) {
          // Check if data is an array
          if (Array.isArray(data)) {
            // Map each measure to a chip and join them
            return `<div class="chips chips--grid">${data.map(measure => `<div class="chip chip--grid">${measure}</div>`).join(" ")}</div>`;
          }
          return '';
        }
      }]
    }

  }

  // This is the method that would accept the reponse data from the api and process it further to be sent to the dt options.
  // This might need some more fixes depending on how the final data looks like.
  processDataForTable(reports: Report[]) {
    return reports.map(report => {

      // basic vars
      const status = report.status,
            periodLength = calculatePeriodLength(report.periodStart, report.periodEnd),
            reportPeriod = this.convertDateString.transform(formatDate(report.periodStart)) + ' - ' + this.convertDateString.transform(formatDate(report.periodEnd))

      // period data
      let periodData;
      if (status === 'Submitted') {
        periodData = [reportPeriod, periodLength.toString() + ' Days'];
      } else {
        periodData = status === "failed" ? "n/a" : "Pending";
      }

      // timestamp
      let timestamp
      if (report.generatedTime && status === 'submitted') {
        timestamp = this.convertDateString.transform(report.generatedTime)
      } else if (report.submittedTime) {
        timestamp = this.convertDateString.transform(report.submittedTime)
      } else {
        timestamp = 'n/a'
      }


      // details
      const details = status === 'Submitted' ? `Bundle<br>#${report.id}` : (status === "Draft" ? "In Progress" : "Error report");

      // measures and activity
      let measuresData,
          activity
      if (status === "Draft") {
        measuresData = "Pending"
        activity = 'Submission Initiated'
      } else if (status === "Failed") {
        measuresData = "n/a";
        activity = 'Failed Submission'
      } else {
        measuresData = report.measureIds;
        activity = 'Successful Submission'
      }

      return {
        ID: report.id,
        STATUS: status,
        TIMESTAMP: timestamp,
        ACTIVITY: activity,
        DETAILS: details,
        FACILITY: {name: report.tenantName, id: report.tenantId},
        NHSN_ORG_ID: report.cdcOrgId,
        REPORTING_PERIOD: periodData,
        MEASURES: report.measureIds.map(m => {
          const measure = this.pascalCaseToSpace.transform(m)
          return measure.split(' ')[0]
        })
      };
    });
  }
}
