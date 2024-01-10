import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { HeroComponent } from 'src/app/shared/hero/hero.component';
import { SectionComponent } from 'src/app/shared/section/section.component';
import { SectionHeadingComponent } from 'src/app/shared/section-heading/section-heading.component';
import { ButtonComponent } from 'src/app/shared/button/button.component';
import { IconComponent } from 'src/app/shared/icon/icon.component';
import { FacilitiesApiService } from 'src/services/api/facilities/facilities-api.service';
import { CardComponent } from "../../../shared/card/card.component";
import { TabComponent } from 'src/app/shared/tab/tab.component';
import { TabContainerComponent } from 'src/app/shared/tab-container/tab-container.component';
import { LinkComponent } from 'src/app/shared/link/link.component';
import { AccordionComponent } from 'src/app/shared/accordion/accordion.component';
import { ReportApiService } from 'src/services/api/report/report-api.service';
import { TableFilter } from 'src/app/shared/interfaces/table.model';
import { calculatePeriodLength, generateRandomData, getPeriodData, getSubmissionStatus, processMeasuresData } from 'src/app/helpers/ReportHelper';
import { Report } from 'src/app/shared/interfaces/report.model';
import { TableComponent } from "../../../shared/table/table.component";
import { MiniContentComponent } from 'src/app/shared/mini-content/mini-content.component';

@Component({
    selector: 'app-facility',
    standalone: true,
    templateUrl: './facility.component.html',
    styleUrls: ['./facility.component.scss'],
    imports: [CommonModule, HeroComponent, SectionComponent, SectionHeadingComponent, ButtonComponent, IconComponent, CardComponent, TabComponent, TabContainerComponent, LinkComponent, AccordionComponent, TableComponent, MiniContentComponent]
})
export class FacilityComponent {
  facilityId: string | null = null
  facilityDetails: any = null;
  isFacilityActivityTableLoaded = false;

  dtOptions: DataTables.Settings = {};
  dtFilters: TableFilter[] = [
    {
      name: 'Sort:',
      options: [
        {
          label: 'ASC',
          value: true
        },
        {
          label: 'DESC',
          value: false
        },
        {
          label: 'Newest First',
          value: true
        },
        {
          label: 'Oldest First',
          value: false
        }
      ]
    }
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private facilitiesApiService: FacilitiesApiService,
    private reportApiService: ReportApiService,
  ) { }

  async ngOnInit() {
    this.route.paramMap.subscribe(params => {
      this.facilityId = params.get('id')

      if (this.facilityId) {
        this.GetFacilityDetails(this.facilityId);
        this.fetchDataForFacilityActivityTable(this.facilityId);
      } else {
        this.router.navigate(['/facilities'])
      }
    })
  }

  async GetFacilityDetails(id: string) {
    try {
      const tenantDetail = await this.facilitiesApiService.fetchFacilityById(id);
      this.facilityDetails = tenantDetail;
    } catch (error) {
      console.error('Error Loading table data.', error);
    }
  }

  async fetchDataForFacilityActivityTable(facilityId: string): Promise<void> {
    this.reportApiService.fetchAllReport({ tenantId: facilityId })
      .then(reports => {
        const transformedData = this.transformData(reports);
        this.dtOptions = this.calculateDtOptions(transformedData);

        this.isFacilityActivityTableLoaded = true;
      })
      .catch(error => {
        console.error('Error fetching reports:', error);
      });
  }

  generateEditLink(): { url: string } {
    const url = this.facilityId
      ? `/facilities/edit-facility/${this.facilityId}`
      : '/facilities/add-facility';

    return { url: url };
  }

  calculateDtOptions(data: any): DataTables.Settings {
    // DataTable configuration
    return {
      data: data,
      pageLength: 15,
      lengthChange: false,
      info: false,
      searching: false,
      scrollX: true,
      stripeClasses: ['zebra zebra--even', 'zebra zebra--odd'],
      columnDefs: [
        {
          targets: 0, // Timestamp
          width: '147px',
          createdCell: (cell, cellData) => {
            $(cell).addClass('timestamp');
          },
          render: function (data, type, row) {
            // Split the timestamp into date and time parts
            const dateTimeParts = data.split(' ');
            const datePart = dateTimeParts[0];
            const timePart = dateTimeParts.slice(1).join(' ');

            return `${datePart}<br>${timePart}`;
          }
        },
        {
          targets: 1, // Activity
          width: '75px',
          createdCell: (cell, cellData) => {
            if (cellData.toLowerCase().includes('initiated')) {
              $(cell).addClass('cell--initiated');
            } else {
              $(cell).addClass('cell--complete');
            }
          }
        },
        {
          targets: 2, // Details
          createdCell: (cell, cellData) => {
            if (cellData.toLowerCase().includes('progress')) {
              $(cell).addClass('cell--initiated');
            } else {
              $(cell).addClass('cell--complete');
            }
          }
        },
        {
          targets: 3, // Facility
          width: '172px'
        },
        {
          targets: [4, 7], // Org Id, Total Census
          width: '144px'
        },
        {
          targets: 5, // Reporting Period
          width: '196px',
          createdCell: (cell, cellData) => {
            if (cellData.toString().toLowerCase() === 'pending') {
              $(cell).addClass('cell--initiated');
            }
          }
        },
      ],
      orderMulti: true,
      columns: [{
        title: 'Timestamp',
        data: 'Timestamp'
      },
      {
        title: 'Activity',
        data: 'Activity',
        render: function (data, type, row) {
          let dotClass = 'neutral';
          if (data.toString().toLowerCase().includes('successful')) {
            dotClass = 'success';
          } else if (data.toString().toLowerCase().includes('failed')) {
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
        data: 'Details'
      },
      {
        title: 'Facility',
        data: 'Facility',
        render: function (data, type, row) {
          return `<a href="/facilities/facility/${row.FacilityId}">${data}</a>`;
        }
      },
      {
        title: 'NHSN Org Id',
        data: 'NHSNOrgId',
        width: '144px'
      },
      {
        title: 'Reporting Period',
        data: 'ReportingPeriod',
        render: function (data, type, row) {
          if (Array.isArray(data)) {
            return `${data[1]}<br>${data[0]}`;
          }
          return data; // 'pending' or 'n/a'
        }
      },
      {
        title: 'Measures',
        data: 'Measures',
        render: function (data, type, row) {
          // Check if data is an array
          if (Array.isArray(data)) {
            // Map each measure to a span and join them
            return `<div class="chips">${data.map(measure => `<div class="chip">${measure}</div>`).join(" ")}</div>`;
          }
          // Applying different classes based on the value ('Pending' or 'n/a')
          let className = '';
          if (data.toString().toLowerCase() === 'pending') {
            className = 'cell--initiated';
          }
          return `<div class="${className}">${data}</div>`;
        }
      },
      {
        title: 'Total In Census',
        data: 'TotalInCensus'
      }]
    }

  }

  // This is the method that would accept the reponse data from the api and process it further to be sent to the dt options.
  // This might need some more fixes depending on how the final data looks like.
  transformData(reports: any[]) {
    return reports.map(report => {
      const status = report?.status.toLowerCase();

      const timestamp = status === "submitted" ? report.submittedTime : report?.periodStart;
      const activity = getSubmissionStatus(status);
      const bundleDetail = status === 'submitted' ? `Bundle #${report.id}` : (status === "failed" ? "Error report" : "In Progress");
      const periodData = getPeriodData(status, report.periodStart, report.periodEnd);
      const totalInCensus = status === 'submitted' ? '123451' : "--";
      const measuresData = processMeasuresData(status, report.measureIds)

      return {
        Id: report.id,
        Status: status,
        Timestamp: timestamp,
        Activity: activity,
        Details: bundleDetail,
        Facility: report.tenantName,
        FacilityId: report.tenantId,
        NHSNOrgId: report.cdcOrgId,
        ReportingPeriod: periodData,
        Measures: measuresData,
        TotalInCensus: totalInCensus
      };
    });
  }
}
