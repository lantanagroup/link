import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { from } from 'rxjs';
import { HeroComponent } from 'src/app/shared/hero/hero.component';
import { IconComponent } from 'src/app/shared/icon/icon.component';
import { ButtonComponent } from 'src/app/shared/button/button.component';
import { SectionComponent } from 'src/app/shared/section/section.component';
import { SectionHeadingComponent } from 'src/app/shared/section-heading/section-heading.component';
import { TableComponent } from 'src/app/shared/table/table.component';
import { Tenant } from 'src/app/shared/interfaces/tenant.model';
import { SearchBar } from 'src/app/shared/interfaces/table.model';
import { FacilitiesApiService } from 'src/services/api/facilities/facilities-api.service';
import { PascalCaseToSpace, ConvertDateString, ConvertToLocaleTime } from 'src/app/helpers/GlobalPipes.pipe';

@Component({
  selector: 'app-facilities',
  standalone: true,
  imports: [CommonModule, HeroComponent, IconComponent, TableComponent, ButtonComponent, SectionComponent, SectionHeadingComponent],
  templateUrl: './facilities.component.html',
  styleUrls: ['./facilities.component.scss']
})
export class FacilitiesComponent implements OnInit {
  constructor(
    private facilitiesApiService: FacilitiesApiService
  ) { }
  private pascalCaseToSpace = new PascalCaseToSpace
  private convertDateString = new ConvertDateString
  private convertToLocaleTime = new ConvertToLocaleTime

  dtOptions: DataTables.Settings = {};
  
  dtSearchBar: SearchBar = {
    title: 'Search Facilities',
    placeholder: 'Enter facility name, NHSN Org ID, etc.'
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
      this.dtOptions = this.calculateDtOptions();
      this.tableOptionsLoaded = true;
    } catch (error) {
      console.error('Error Loading table data.', error);
    }
  }

  calculateDtOptions(): DataTables.Settings {
    // DataTable configuration
    const columnIdMap = ['NAME', 'NHSN_ORG_ID', 'DETAILS', 'SUBMISSION_DATE', 'MEASURES'],
          pageLength = 5

    return {
      serverSide: true,
      processing: true,
      ajax: (dataTablesParameters: any, callback) => {
        const page = Math.ceil(dataTablesParameters.start / pageLength) + 1

        let order = dataTablesParameters.order[0],
            orderBy = columnIdMap[order.column],
            sortAscend = order.dir === 'asc'

        let searchValue = dataTablesParameters.search.value

        from(this.facilitiesApiService.fetchAllFacilities({page: page, sort: orderBy, sortAscend: sortAscend, searchCriteria: searchValue}))
          .subscribe(response => {
            callback({
              recordsTotal: response?.tenants.length,
              recordsFiltered: response?.total,
              data: this.processDataForTable(response?.tenants)
            })
          })
      },
      pageLength: pageLength,
      lengthChange: false,
      info: false,
      orderMulti: true,
      searching: false,
      scrollX: true,
      stripeClasses: ['zebra zebra--even', 'zebra zebra--odd'],
      columns: [
        {
          title: 'Facility Name',
          data: columnIdMap[0],
          orderable: true,
          render: function (data, type, row) {
            return `<a href="/facilities/facility/${row.FACILITY_ID}">${data}</a>`;
          }
        },
        {
          title: 'NHSN Org Id',
          data: columnIdMap[1],
          orderable: true
        },
        {
          title: 'Details',
          data: columnIdMap[2],
          orderable: false,
          createdCell: (cell, cellData) => {
            if (cellData.toLowerCase().includes('progress')) {
              $(cell).addClass('cell--initiated');
            } else {
              $(cell).addClass('cell--complete');
            }
          },
          render: function (data, type, row) {
            return `<a href="/activities/bundle/${row.FACILITY_ID}/${row.DETAILS}">Bundle<br>#${data}</a>`
          }
        },
        {
          title: 'Last Submission',
          data: columnIdMap[3],
          orderable: true,
          render: function(data, type, row) {
            let parts = data.split(' ', 2)
            return parts[0] + '<br>' + data.substring(parts[0].length).trim()
          }
        },
        {
          title: 'Current Measures',
          data: columnIdMap[4],
          orderable: false,
          render: function (data, type, row) {
            // Check if data is an array
            if (Array.isArray(data)) {
              // Map each measure to a chip and join them
              return `<div class="chips chips--grid">${data.map(measure => `<div class="chip chip--grid">${measure}</div>`).join(" ")}</div>`;
            }
            return '';
          }
        }
      ]
    }

  }

  // This is the method that would accept the reponse data from the api and process it further to be sent to the dt options.
  processDataForTable(tenantsData: Tenant[] | undefined) {
    if(!tenantsData) 
      return

    return tenantsData.map(td => {

      let submissionDate = td.lastSubmissionDate

      if(submissionDate) {
        // const [datePart, timePart] = td.lastSubmissionDate.split(' '),
        //       transformedDate = this.convertDateString.transform(datePart)

        // submissionDate = `${transformedDate} ${timePart}`
        submissionDate = this.convertToLocaleTime.transform(submissionDate)
      }

      return {
        FACILITY_ID: td.id,
        NAME: td.name,
        NHSN_ORG_ID: td.nhsnOrgId,
        DETAILS: td.lastSubmissionId,
        SUBMISSION_DATE: submissionDate,
        MEASURES: td.measures
          .filter(m => m && m.shortName)
          .map(m => {
            const measure = this.pascalCaseToSpace.transform(m.shortName.trimStart())
            return measure.split(' ')[0]
          })
      };
    });
  }
}
