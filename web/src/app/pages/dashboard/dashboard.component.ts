import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { from } from 'rxjs';
import { HeroComponent } from 'src/app/shared/hero/hero.component';
import { SectionHeadingComponent } from 'src/app/shared/section-heading/section-heading.component';
import { CardComponent } from 'src/app/shared/card/card.component';
import { MetricComponent } from 'src/app/shared/metric/metric.component';
import { ButtonComponent } from 'src/app/shared/button/button.component';
import { IconComponent } from 'src/app/shared/icon/icon.component';
import { TableComponent } from "../../shared/table/table.component";
import { SectionComponent } from 'src/app/shared/section/section.component';
import { Report } from 'src/app/shared/interfaces/report.model';

import { ReportApiService } from 'src/services/api/report/report-api.service';
import { MetricApiService } from 'src/services/api/metric/metric-api.service';
import { MetricCard, TimePeriod } from 'src/app/shared/interfaces/metrics.model';

import { ConvertToLocaleTime } from 'src/app/helpers/GlobalPipes.pipe';

@Component({
    selector: 'app-dashboard',
    standalone: true,
    templateUrl: './dashboard.component.html',
    styleUrls: ['./dashboard.component.scss'],
    imports: [CommonModule, HeroComponent, ButtonComponent, IconComponent, CardComponent, MetricComponent, SectionHeadingComponent, TableComponent, SectionComponent]
})
export class DashboardComponent {
  metricCards: MetricCard[] = [];
  completedDtOptions: DataTables.Settings | null = null;
  failedDtOptions: DataTables.Settings | null = null;
  pendingDtOptions: DataTables.Settings | null = null;
  cardCount: number = 3;
  columnSpan: number = 4;

  private convertToLocaleTime = new ConvertToLocaleTime

  constructor(
    private reportsApiService: ReportApiService,
    private metricApiService: MetricApiService
  ) {}

  ngOnInit(): void {
    // recent activity cards
    this.generateMetricCards()


    // tables
    this.completedDtOptions = this.calculateDtOptions('Submitted')
    this.pendingDtOptions = this.calculateDtOptions('Draft')
    // this.failedDtOptions = this.calculateDtOptions('Failed')
    // count the number of tables we want to render
    this.cardCount = 2
    this.columnSpan = 12 / this.cardCount
  }
  // ************** Recent activity cards ************ //

  async generateMetricCards() {
    // fetch all the metric data
    try {
      const metricData = await this.metricApiService.fetchMetrics(TimePeriod.LastWeek)
      // loop through and assign cards' data: 'queryTime', 'patientsQueried', 'validation'
      const cards = [
        {
          name: 'Average Query Time',
          subText: 'on average past 7 days',
          changeWindow: 'last week',
          upGood: false,
          toTimestamp: true,
          metricData: metricData?.queryTime
        },
        {
          name: 'Total Patients Queried',
          subText: 'patients queried past 7 days',
          changeWindow: 'last week',
          upGood: true,
          toTimestamp: false,
          metricData: metricData?.patientsQueried
        },
        {
          name: 'Average Validation Time',
          subText: 'on average past 7 days',
          changeWindow: 'last week',
          upGood: false,
          toTimestamp: true,
          metricData: metricData?.validation
        }
      ]

      this.metricCards = cards
    } catch (error) {
      console.error('Error loading metric card data:', error)
    }
  }


  // ************** Tables ************ //
  // This method accepts data and calculates the dataTable options
  calculateDtOptions(status: 'Draft' | 'Submitted' | 'Failed'): DataTables.Settings {
    // DataTable configuration
    const columnIdMap = ['DETAILS', 'FACILITY', 'TIMESTAMP'],
          pageLength = 5

    return {
      serverSide: true,
      processing: true,
      ajax: (dataTablesParameters: any, callback) => {
        const apiFilters = {
          page: 1,
          count: pageLength,
          status: status
        }

        from(this.reportsApiService.fetchAllReports(apiFilters))
          .subscribe(response => {
            callback({
              data: this.processDataForTable(response)
            })
          })
      },
      paging: false,
      lengthChange: false,
      info: false,
      searching: false,
      columns: [
        {
          title: 'Details',
          data: columnIdMap[0],
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
          data: columnIdMap[1],
          width: '200px',
          render: function (data, type, row) {
            return `<a href="/facilities/facility/${data.id}">${data.name}</a>`;
          }
        },
        {
          title: 'Timestamp',
          data: columnIdMap[2],
          createdCell: (cell, cellData) => {
            $(cell).addClass('timestamp')
          },
          render: function(data, type, row) {
            let parts = data.split(' ', 2)
            return parts[0] + '<br>' + data.substring(parts[0].length).trim()
          }
        }
      ]
    }

  }

  // This is the method that would accept the reponse data from the api and process it further to be sent to the dt options.
  // This might need some more fixes depending on how the final data looks like.
  processDataForTable(reports: Report[]): any[] {
    return reports.map(report => {

      const status = report.status;

      // details
      const details = status === 'Submitted' ? `Bundle<br>#${report.id}` : (status === "Draft" ? "In Progress" : "Error report");

      // timestamp
      let timestamp
      if (report.generatedTime && status === 'submitted') {
        timestamp = this.convertToLocaleTime.transform(report.generatedTime)
      } else if (report.submittedTime) {
        timestamp = this.convertToLocaleTime.transform(report.submittedTime)
      } else {
        timestamp = 'n/a'
      }

      return {
        ID: report.id,
        STATUS: status,
        DETAILS: details,
        FACILITY: {name: report.tenantName, id: report.tenantId},
        TIMESTAMP: timestamp,
      };
    });
  }
}
