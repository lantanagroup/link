import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HeroComponent } from 'src/app/shared/hero/hero.component';
import { CardComponent } from 'src/app/shared/card/card.component';
import { MetricComponent } from 'src/app/shared/metric/metric.component';
import { DataTablesModule } from 'angular-datatables';
import { TableComponent } from 'src/app/shared/table/table.component';
import { IconComponent } from "../../shared/icon/icon.component";
import { ButtonComponent } from "../../shared/button/button.component";
import { SectionHeadingComponent } from "../../shared/section-heading/section-heading.component";
import { Report } from 'src/app/shared/interfaces/report.model';
import { calculatePeriodLength, generateRandomData } from 'src/app/helpers/ReportHelper';

@Component({
  selector: 'app-activities',
  standalone: true,
  templateUrl: './activities.component.html',
  styleUrls: ['./activities.component.scss'],
  imports: [CommonModule, DataTablesModule, HeroComponent, CardComponent, MetricComponent, TableComponent, IconComponent, ButtonComponent, SectionHeadingComponent]
})
export class ActivitiesComponent implements OnInit {
  dtOptions: DataTables.Settings = {};

  ngOnInit(): void {
    // Step 1: Generate random data
    const randomData = generateRandomData(50);

    // Step 2: Transform the data
    const transformedData = this.transformData(randomData);

    // Step 3: Calculate DataTable options with the transformed data
    this.dtOptions = this.calculateDtOptions(transformedData);
  }

  // This method will be replaced by something else
  showAlert(): void {
    alert('Filters coming out soon.');
  }


  calculateDtOptions(data: any): DataTables.Settings {
    // DataTable configuration
    return {
      data: data,
      pageLength: 15,
      lengthChange: false,
      info: false,
      searching: false,
      columnDefs: [
        {
          targets: 0, // Timestamp
          width: '147px',
          createdCell: (cell, cellData) => {
            $(cell).addClass('table-default-font-style timestamp-padding');
          },
          render: function (data, type, row) {
            // Split the timestamp into date and time parts
            const dateTimeParts = data.split(' ');
            const datePart = dateTimeParts[0];
            const timePart = dateTimeParts.slice(1).join(' ');

            return `<div>${datePart}</div><div>${timePart}</div>`;
          }
        },
        {
          targets: 1, // Activity
          width: '75px',
          createdCell: (cell, cellData) => {
            if (cellData.toLowerCase().includes('initiated')) {
              $(cell).addClass('activity-initiated');
            } else {
              $(cell).addClass('activity-success-failed');
            }
          }
        },
        {
          targets: 2, // Details
          createdCell: (cell, cellData) => {
            if (cellData.toLowerCase().includes('progress')) {
              $(cell).addClass('details-inprogress');
            } else {
              $(cell).addClass('details-bundle');
            }
          }
        },
        {
          targets: 3, // Facility
          width: '172px',
          createdCell: (cell, cellData) => {
            $(cell).addClass('facility-regular');
          }
        },
        {
          targets: [4, 7], // Org Id, Total Census
          width: '144px',
          createdCell: (cell, cellData) => {
            $(cell).addClass('table-default-font-style');
          }
        },
        {
          targets: 5, // Reporting Period
          width: '196px',
          createdCell: (cell, cellData) => {
            if (cellData.toString().toLowerCase() === 'pending') {
              $(cell).addClass('reportingPeriod-pending');
            } else if (cellData.toString().toLowerCase() === 'n/a') {
              $(cell).addClass('table-default-font-style');
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
          let dotClass = 'dot dot--neutral';
          if (data.toString().toLowerCase().includes('successful')) {
            dotClass = 'dot dot--success';
          } else if (data.toString().toLowerCase().includes('failed')) {
            dotClass = 'dot dot--failed'
          }
          return `<div class="activity-container">
                  <div class="${dotClass}"></div>
                  <div class="activity-text">${data}</div>
                </div>`;
        }
      },
      {
        title: 'Details',
        data: 'Details'
      },
      {
        title: 'Facility',
        data: 'Facility'
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
            return `<div class="table-default-font-style">${data[1]}</div><div class="table-default-font-style">${data[0]}</div>`;
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
            return `<div class="measures-container">${data.map(measure => `<span class="measure-span">${measure}</span>`).join(" ")}</div>`;
          }
          // Applying different classes based on the value ('Pending' or 'n/a')
          let className = '';
          if (data.toString().toLowerCase() === 'pending') {
            className = 'reportingPeriod-pending';
          } else if (data.toString().toLowerCase() === 'n/a') {
            className = 'table-default-font-style';
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
    console.log("inside transformData()", reports);
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
