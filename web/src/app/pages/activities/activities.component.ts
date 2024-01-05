import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HeroComponent } from 'src/app/shared/hero/hero.component';
import { CardComponent } from 'src/app/shared/card/card.component';
import { MetricComponent } from 'src/app/shared/metric/metric.component';
import { DataTablesModule } from 'angular-datatables';
import { TableComponent } from 'src/app/shared/table/table.component';
import { SectionComponent } from 'src/app/shared/section/section.component';
import { Report } from 'src/app/shared/interfaces/report.model';
import { calculatePeriodLength, generateRandomData } from 'src/app/helpers/ReportHelper';
import { SearchBar, TableFilter } from 'src/app/shared/interfaces/table.model';

@Component({
  selector: 'app-activities',
  standalone: true,
  templateUrl: './activities.component.html',
  styleUrls: ['./activities.component.scss'],
  imports: [CommonModule, DataTablesModule, HeroComponent, CardComponent, MetricComponent, TableComponent, SectionComponent]
})
export class ActivitiesComponent implements OnInit {
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

  ngOnInit(): void {
    // Step 1: Generate random data
    const randomData = generateRandomData(50);

    // Step 2: Transform the data
    const transformedData = this.transformData(randomData);

    // Step 3: Calculate DataTable options with the transformed data
    this.dtOptions = this.calculateDtOptions(transformedData);
  }


  calculateDtOptions(data: any): DataTables.Settings {
    // DataTable configuration

    console.log('data:', data)

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
        data: 'Details',
        render: function (data, type, row) {
          let bundleId = data.split('#')[1]
          if(bundleId) {
            return `<a href="/activities/bundle/ehr-test/${bundleId}">${data}</a>`
          }
          return data
        }
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
  transformData(reports: Report[]) {
    return reports.map(report => {
      const reportId = report.reportId;
      const timestamp = report.status === "completed" ? report.generatedTime : report.submittedTime;
      const activity = report.activity;
      const status = report.status;
      const facility = report.tenantName;
      const nhsnOrgId = report.nhsnOrgId;
      const periodLength = calculatePeriodLength(report.periodStart, report.periodEnd);
      const reportPeriod = `${report.periodStart} - ${report.periodEnd}`;
      const totalInCensus = (status === "completed") ? report.aggregates.reduce((sum, value) => sum + parseFloat(value.replace(/,/g, '')), 0).toLocaleString() : "--";


      let periodData;
      if (status === "completed") {
        periodData = [reportPeriod, periodLength.toString() + ' Days'];
      } else {
        periodData = status === "failed" ? "n/a" : "Pending";
      }

      const details = status === "completed" ? `Bundle #${report.details}` : (status === "pending" ? "In Progress" : "Error report");

      let measuresData;
      if (status === "pending") {
        measuresData = "Pending";
      } else if (status === "failed") {
        measuresData = "n/a";
      } else {
        measuresData = report.measureId;
      }

      return {
        ReportId: reportId,
        Status: status,
        Timestamp: timestamp,
        Activity: activity,
        Details: details,
        Facility: facility,
        NHSNOrgId: nhsnOrgId,
        ReportingPeriod: periodData,
        Measures: measuresData,
        TotalInCensus: totalInCensus
      };
    });
  }
}
