import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HeroComponent } from 'src/app/shared/hero/hero.component';
import { IconComponent } from 'src/app/shared/icon/icon.component';
import { ButtonComponent } from 'src/app/shared/button/button.component';
import { SectionComponent } from 'src/app/shared/section/section.component';
import { SectionHeadingComponent } from 'src/app/shared/section-heading/section-heading.component';
import { TableComponent } from 'src/app/shared/table/table.component';
import { DataService } from 'src/services/api/data.service';
import { Tenant } from 'src/app/shared/interfaces/tenant.model';
import { TableFilter, SearchBar } from 'src/app/shared/interfaces/table.model';
import { FacilitiesApiService } from 'src/services/api/facilities/facilities-api.service';

@Component({
  selector: 'app-facilities',
  standalone: true,
  imports: [CommonModule, HeroComponent, IconComponent, TableComponent, ButtonComponent, SectionComponent, SectionHeadingComponent],
  templateUrl: './facilities.component.html',
  styleUrls: ['./facilities.component.scss']
})
export class FacilitiesComponent implements OnInit {
  constructor(private dataService: DataService, private facilitiesApiService: FacilitiesApiService) { }
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
  dtSearchBar: SearchBar = {
    title: 'Search Facilities',
    placeholder: 'Enter facility name, CDC ID, etc.'
  };
  tableOptionsLoaded = false;

  // This method will be replaced by something else
  showAlert(): void {
    alert('Filters coming out soon.');
  }

  async ngOnInit(): Promise<void> {
    await this.LoadFacilitiesTableData();
  }

  async LoadFacilitiesTableData() {
    try {
      const tenants = await this.facilitiesApiService.fetchAllFacilities();
      const transformedData = this.processDataForTable(tenants);
      this.dtOptions = this.calculateDtOptions(transformedData);
      this.tableOptionsLoaded = true;
    } catch (error) {
      console.error('Error Loading table data.', error);
    }
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
          targets: 0, // Facility
          width: '172px'
        },
        {
          targets: [1, 3], // Org Id, LastSubmission
          width: '144px'
        },
        {
          targets: 2, // Details
          createdCell: (cell, cellData) => {
            if (cellData.toLowerCase().includes('progress')) {
              $(cell).addClass('cell--initiated');
            } else {
              $(cell).addClass('cell--complete');
            }
          },
          render: function (data, type, row) {
            // Split the timestamp into date and time parts
            if (!data.toLowerCase().includes('progress')) {
              const dataParts = data.split(' ').join('<br>');
              return dataParts
            }
            return data
          }
        },
      ],
      orderMulti: true,
      columns: [
        {
          title: 'Facility Name',
          data: 'FacilityName'
        },
        {
          title: 'NHSN Org Id',
          data: 'NHSNOrgId',
        },
        {
          title: 'Details',
          data: 'LastSubmissionId'
        },
        {
          title: 'Last Submission',
          data: 'LastSubmission'
        },
        {
          title: 'Current Measures',
          data: 'Measures',
          render: function (data, type, row) {
            debugger;
            // Check if data is an array
            if (Array.isArray(data)) {
              // Map each measure to a chip and join them
              return `<div class="chips">${data.map(measure => `<div class="chip">${measure}</div>`).join(" ")}</div>`;
            }
            return '';
          }
        }
      ]
    }

  }

  // This is the method that would accept the reponse data from the api and process it further to be sent to the dt options.
  processDataForTable(tenantsData: Tenant[]) {
    return tenantsData.map(td => {
      const lastSubmission = td.lastSubmissionDate;
      const facilityName = td.name;
      const nhsnOrgId = td.nhsnOrgId;
      const lastSubmissionId = `Bundle #${td.lastSubmissionId}`;
      const measuresData = td.measures.map(m => m.shortName.slice(0, 4));

      return {
        FacilityName: facilityName,
        NHSNOrgId: nhsnOrgId,
        LastSubmissionId: lastSubmissionId,
        LastSubmission: lastSubmission,
        Measures: measuresData,
      };
    });
  }
}
