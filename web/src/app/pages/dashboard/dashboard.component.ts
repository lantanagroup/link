import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HeroComponent } from 'src/app/shared/hero/hero.component';
import { SectionHeadingComponent } from 'src/app/shared/section-heading/section-heading.component';
import { CardComponent } from 'src/app/shared/card/card.component';
import { MetricComponent } from 'src/app/shared/metric/metric.component';
import { ButtonComponent } from 'src/app/shared/button/button.component';
import { IconComponent } from 'src/app/shared/icon/icon.component';
import { TableComponent } from "../../shared/table/table.component";
import { Report } from 'src/app/shared/interfaces/report.model';
import { generateRandomData } from 'src/app/helpers/ReportHelper';
import { recentActivityData } from 'src/app/helpers/RecentActivityHelper';
import { SectionComponent } from 'src/app/shared/section/section.component';

@Component({
    selector: 'app-dashboard',
    standalone: true,
    templateUrl: './dashboard.component.html',
    styleUrls: ['./dashboard.component.scss'],
    imports: [CommonModule, HeroComponent, ButtonComponent, IconComponent, CardComponent, MetricComponent, SectionHeadingComponent, TableComponent, SectionComponent]
})
export class DashboardComponent {
  completedDtOptions: DataTables.Settings = {};
  failedDtOptions: DataTables.Settings = {};
  pendingDtOptions: DataTables.Settings = {};

  recentActivityData = recentActivityData

  ngOnInit(): void {
    // Step 1: Generate random data
    const randomData = generateRandomData(50);

    // Step 3: Transform the data
    const completedData = this.transformData(randomData.filter(x => x.status === "completed").slice(0, 5));
    const pendingData = this.transformData(randomData.filter(x => x.status === "pending").slice(0, 5));
    const failedData = this.transformData(randomData.filter(x => x.status === "failed").slice(0, 5));

    // Step 3: Calculate DataTable options with the transformed data
    this.completedDtOptions = this.calculateDtOptions(completedData);
    this.failedDtOptions = this.calculateDtOptions(failedData);
    this.pendingDtOptions = this.calculateDtOptions(pendingData);
  }

  // This method accepts data and calculates the dataTable options
  calculateDtOptions(data: any): DataTables.Settings {
    // DataTable configuration
    return {
      data: data,
      initComplete: function() {
        $('table.dataTable thead').hide(); //Hide table header
      },
      paging: false,
      pageLength: 1,
      lengthChange: false,
      info: false,
      searching: false,
      columnDefs: [
        {
          targets: 0, // Details
          className: 'recent-activity--details',
          render: function (data, type, row) {
            return `<a href="#">${data}</a>`
          },
          createdCell: (cell, cellData) => {
            if (cellData.toLowerCase().includes('progress')) {
              $(cell).addClass('cell--inProgress cell--initiated');
            } else {
              $(cell).addClass('cell--complete');
            }
          }
        },
        {
          targets: 1, // Facility
          'width': '200px',
          render: function (data, type, row) {
            return `<a href="#">${data}</a>`
          }
        },
        {
          targets: 2, // Timestamp
          render: function (data, type, row) {
            // Split the timestamp into date and time parts
            const dateTimeParts = data.split(' ');
            const datePart = dateTimeParts[0];
            const timePart = dateTimeParts.slice(1).join(' ');

            return `${datePart}<br>${timePart}`;
          }
        }
      ],
      columns: [
        {
          title: 'Details',
          data: 'Details',
        },
        {
          title: 'Facility',
          data: 'Facility',
        },
        {
          title: 'Timestamp',
          data: 'Timestamp',
        }
      ]
    }

  }

  // This is the method that would accept the reponse data from the api and process it further to be sent to the dt options.
  // This might need some more fixes depending on how the final data looks like.
  transformData(reports: Report[]): any[] {
    return reports.map(report => {
      const reportId = report.reportId;
      const timestamp = report.status === "completed" ? report.generatedTime : report.submittedTime;
      const status = report.status;
      const facility = report.tenantName;
      const details = status === "completed" ? `Bundle #${report.details}` : (status === "pending" ? "In Progress" : "Report (12 Errors)");

      return {
        ReportId: reportId,
        Timestamp: timestamp,
        Details: details,
        Facility: facility,
      };
    });
  }
}
