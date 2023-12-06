import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HeroComponent } from 'src/app/shared/hero/hero.component';
import { CardComponent } from 'src/app/shared/card/card.component';
import { MetricComponent } from 'src/app/shared/metric/metric.component';
import { DataTablesModule } from 'angular-datatables';
import { TableComponent } from 'src/app/shared/table/table.component';

interface Report {
  id: string;
  measureId: string[];
  periodStart: string;
  periodEnd: string;
  status: string;
  submittedTime: string;
  generatedTime: string;
  version: string;
  patientLists: string[];
  aggregates: string[];
  tenantName: string;
  nhsnOrgId: string;
  reportId: string;
  activity: string;
  details: string;
};

type StatusType = 'pending' | 'completed' | 'failed';

@Component({
  selector: 'app-activities',
  standalone: true,
  imports: [CommonModule, DataTablesModule, HeroComponent, CardComponent, MetricComponent, TableComponent],
  templateUrl: './activities.component.html',
  styleUrls: ['./activities.component.scss']
})
export class ActivitiesComponent implements OnInit {
  dtOptions: DataTables.Settings = {};

  ngOnInit(): void {
    // Step 1: Generate random data
    const randomData = this.generateRandomData(50);

    // Step 2: Transform the data
    const transformedData = this.transformData(randomData);

    // Step 3: Calculate DataTable options with the transformed data
    this.dtOptions = this.calculateDtOptions(transformedData);
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
      const periodLength = this.calculatePeriodLength(report.periodStart, report.periodEnd);
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

  // Below funcitons are all helper functions. All of these function could be moved to a separate helper file or something.
  generateRandomData(numEntries: number): Report[] {
    const facilities = ["University of Minnesota", "University of South Dakota", "University of Wisconsin"];
    const measuresArray = ["ABC", "DE", "ABE", "REW"];
    const statusOptions: StatusType[] = ["pending", "completed", "failed"];
    const activitiesMap: { [K in StatusType]: string } = {
      "pending": "Submission Initiated",
      "completed": "Successful Submission",
      "failed": "Failed Submission"
    };

    const randomData: Report[] = [];

    for (let i = 0; i < numEntries; i++) {
      const status: StatusType = statusOptions[Math.floor(Math.random() * statusOptions.length)];
      const activity = activitiesMap[status];
      const measures = status === 'completed' ? this.getRandomSubarray(measuresArray, Math.ceil(Math.random() * measuresArray.length)) : (status === 'pending' ? ['Pending'] : ['n/a']);
      const aggregates = status === 'completed' ? this.generateRandomNumbersAsStrings(5) : ['--'];
      const { periodStart, periodEnd } = this.generateRandomPeriod();

      randomData.push({
        id: this.createGUID(),
        reportId: this.createGUID(),
        submittedTime: this.generateRandomDateTime(),
        generatedTime: this.generateRandomDateTime(),
        activity: activity,
        details: (Math.floor(Math.random() * (9999999 - 1000000 + 1)) + 1000000).toString(),
        tenantName: facilities[Math.floor(Math.random() * facilities.length)],
        nhsnOrgId: (Math.floor(Math.random() * (9999999 - 1000000 + 1)) + 1000000).toString(),
        periodStart: periodStart,
        periodEnd: periodEnd,
        measureId: measures,
        aggregates: aggregates,
        status: status,
        version: "1.0",
        patientLists: [],
      });
    }
    return randomData;
  }

  getMeasuresBasedOnStatus(status: any, measuresOptions: any) {
    switch (status) {
      case 'pending':
        return ['Pending'];
      case 'failed':
        return ['n/a'];
      case 'completed':
        return this.getRandomSubarray(measuresOptions, Math.ceil(Math.random() * 3)); // Up to 3 measures
      default:
        return [];
    }
  }

  createGUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
      var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  }
  generateRandomNumbersAsStrings(count: number): string[] {
    const numbers = [];
    for (let i = 0; i < count; i++) {
      // Generate a random number between 10,000 (inclusive) and 99,999 (inclusive)
      const randomNum = Math.floor(Math.random() * (9999 - 1000 + 1)) + 1000;
      numbers.push(randomNum.toLocaleString()); // Convert to string with commas
    }
    return numbers;
  }

  generateRandomPeriod() {
    const start = new Date(2023, Math.floor(Math.random() * 12), Math.floor(Math.random() * 28) + 1);
    const end = new Date(start);
    end.setDate(end.getDate() + 7);

    const formatDate = (date: any) => `${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}.${String(date.getFullYear()).substring(2)}`;
    return {
      periodStart: formatDate(start),
      periodEnd: formatDate(end)
    };
  }

  generateRandomDateTime() {
    return `03.14.2023 ${Math.floor(Math.random() * 24)}:${Math.floor(Math.random() * 60)} ${Math.random() > 0.5 ? "AM" : "PM"} EST`;
  }

  getRandomSubarray(arr: any[], size: number) {
    const shuffled = arr.slice(0);
    let i = arr.length;
    const min = i - size;

    while (i-- > min) {
      const index = Math.floor((i + 1) * Math.random());
      const temp = shuffled[index];
      shuffled[index] = shuffled[i];
      shuffled[i] = temp;
    }

    return shuffled.slice(min);
  }

  calculatePeriodLength(periodStart: string, periodEnd: string): number {
    // Parse the dates from the format "MM.DD.YY" to JavaScript Date objects
    const startDate: Date = this.parseDate(periodStart);
    const endDate: Date = this.parseDate(periodEnd);

    // Calculate the difference in milliseconds and convert to days
    const diffInMilliseconds: number = endDate.getTime() - startDate.getTime();
    const diffInDays: number = diffInMilliseconds / (1000 * 60 * 60 * 24);

    return Math.round(diffInDays); // rounding to the nearest whole number
  }

  parseDate(dateStr: string): Date {
    const [month, day, year] = dateStr.split('.').map(Number);
    // Adjust year format from YY to YYYY (assuming 2000s)
    const fullYear: number = 2000 + year;
    return new Date(fullYear, month - 1, day); // Month is 0-indexed in JavaScript Date
  }
}
