import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { from } from 'rxjs';
import { HeroComponent } from 'src/app/shared/hero/hero.component';
import { CardComponent } from 'src/app/shared/card/card.component';
import { MetricComponent } from 'src/app/shared/metric/metric.component';
import { DataTablesModule } from 'angular-datatables';
import { TableComponent } from 'src/app/shared/table/table.component';
import { SectionComponent } from 'src/app/shared/section/section.component';
import { Report } from 'src/app/shared/interfaces/report.model';
import { SearchBar } from 'src/app/shared/interfaces/table.model';
import { ReportApiService } from 'src/services/api/report/report-api.service';
import { MetricApiService } from 'src/services/api/metric/metric-api.service';
import { MetricCard, TimePeriod } from 'src/app/shared/interfaces/metrics.model';

import { calculatePeriodLength, formatDate } from 'src/app/helpers/ReportHelper';
import { PascalCaseToSpace, ConvertDateString } from 'src/app/helpers/GlobalPipes.pipe';
import { LoaderComponent } from 'src/app/shared/loader/loader.component';

@Component({
  selector: 'app-activities',
  standalone: true,
  templateUrl: './activities.component.html',
  styleUrls: ['./activities.component.scss'],
  imports: [CommonModule, DataTablesModule, HeroComponent, CardComponent, MetricComponent, TableComponent, SectionComponent, LoaderComponent]
})
export class ActivitiesComponent implements OnInit {
  metricCards: MetricCard[] = [];
  dtOptions: DataTables.Settings = {};
  dtSearchBar: SearchBar = {
    title: 'Search Activities',
    placeholder: 'Enter facility name, Bundle ID, Status, etc.'
  };
  isDataLoaded: boolean = false;
  private pascalCaseToSpace = new PascalCaseToSpace
  private convertDateString = new ConvertDateString

  constructor(
    private reportsApiService: ReportApiService,
    private metricApiService: MetricApiService
  ) {}

  async ngOnInit() {
    this.generateMetricCards()
    this.dtOptions = this.calculateDtOptions();
  }

  // ************** Recent activity cards ************ //

  async generateMetricCards() {
    // fetch all the metric data
    try {
      const metricData = await this.metricApiService.fetchMetrics(TimePeriod.LastWeek)
      // loop through and assign cards' data: 'queryTime', 'patientsQueried', 'validation'
      const cards = [
        {
          name: 'Total Patients Reported',
          subText: 'patients reported past 7 days',
          changeWindow: 'yesterday',
          upGood: true,
          toTimestamp: false,
          metricData: metricData?.patientsReported
        },
        {
          name: 'Average Evaluations',
          subText: 'evaluations on average past 7 days',
          changeWindow: 'yesterday',
          upGood: true,
          toTimestamp: false,
          metricData: metricData?.evaluation
        },
        {
          name: 'Average Query Time',
          subText: 'on average past 7 days',
          changeWindow: 'yesterday',
          upGood: false,
          toTimestamp: true,
          metricData: metricData?.queryTime
        },
        {
          name: 'Average Validation Time',
          subText: 'on average past 7 days',
          changeWindow: 'yesterday',
          upGood: false,
          toTimestamp: true,
          metricData: metricData?.validation
        }
      ]

      this.metricCards = cards
      this.isDataLoaded = true
    } catch (error) {
      console.error('Error loading metric card data:', error)
    }
  }


  calculateDtOptions(): DataTables.Settings {
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
      orderMulti: true,
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
            reportPeriod = formatDate(report.periodStart) + ' - ' + formatDate(report.periodEnd)

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
