import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { HeroComponent } from 'src/app/shared/hero/hero.component';
import { ButtonComponent } from 'src/app/shared/button/button.component';
import { SectionComponent } from 'src/app/shared/section/section.component';
import { TabContainerComponent } from 'src/app/shared/tab-container/tab-container.component';
import { TabComponent } from 'src/app/shared/tab/tab.component';
import { CardComponent } from 'src/app/shared/card/card.component';
import { MiniContentComponent } from 'src/app/shared/mini-content/mini-content.component';
import { IconComponent } from 'src/app/shared/icon/icon.component';
import { TableComponent } from 'src/app/shared/table/table.component';
import { LinkComponent } from 'src/app/shared/link/link.component';
import { LoaderComponent } from 'src/app/shared/loader/loader.component';
import { GlobalApiService } from 'src/services/api/globals/globals-api.service';

/* dummy data */
import { normalizationData } from 'src/app/helpers/ReportHelper';

@Component({
  selector: 'app-bundle',
  standalone: true,
  imports: [CommonModule, HeroComponent, ButtonComponent, SectionComponent, TabContainerComponent, TabComponent, CardComponent, MiniContentComponent, IconComponent, TableComponent, LinkComponent, LoaderComponent],
  templateUrl: './bundle.component.html',
  styleUrls: ['./bundle.component.scss']
})
export class BundleComponent {
  bundleId: string | null = '362574'
  tenantId: string | null = 'ehr-test'
  dtOptions: DataTables.Settings = {}
  bundleDetails: any = {}
  isDataLoaded: boolean = false


  // purely placeholder
  mockBundleDetails: any = {
    submittedOn: '01.04.24',
    reportingPeriod: '03.23.23 - 03.30.23',
    facility: {
      name: 'EHR Test On Prem',
      facilityId: this.tenantId,
      cdcOrgId: '1234'
    },
    normalizations: {
      methods: [
        {
          name: 'Code System Cleanup',
          value: 'No'
        },
        {
          name: 'Contained Resource Cleanup',
          value: 'No'
        },
        {
          name: 'Copy Location to Identifier Type',
          value: 'No'
        },
        {
          name: 'Encounter Status Transformer',
          value: 'No'
        },
        {
          name: 'Fixed Period Dates',
          value: 'No'
        },
        {
          name: 'Fix Resource Ids',
          value: 'No'
        },
        {
          name: 'Patient Data Resource Filter',
          value: 'No'
        }
      ],
      details: normalizationData
    }
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private globalApiService: GlobalApiService
  ) {}

  async ngOnInit() {
    this.route.paramMap.subscribe(params => {
      this.bundleId = params.get('bundleId')
      this.tenantId = params.get('tenantId')

      this.dtOptions = this.calculateDtOptions(this.mockBundleDetails.normalizations.details)

      if(this.bundleId && this.tenantId) {
        
        forkJoin({
          bundleDetails: this.globalApiService.getContentObservable(`${this.tenantId}/report/${this.bundleId}/aggregate`),
          // other stuff, if needed
        }).subscribe(({ bundleDetails }) => {
          // currently this endpoint doesn't return anything, but someday
          this.bundleDetails = bundleDetails
          
          this.isDataLoaded = true
        })
      } else {
        this.router.navigate(['/activities'])
      }
    })
  }

  generateFacilityLink(): { url: string } {
    const url = this.tenantId
      ? `/facilities/facility/${this.tenantId}`
      : '/facilities/';

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
      stripeClasses: ['zebra zebra--even', 'zebra zebra--odd'],
      columnDefs: [
        {
          targets: 0, // Timestamp
        },
        {
          targets: 1, // Column 2
        },
        {
          targets: 2, // Column 3
          createdCell: (cell, cellData) => {
            $(cell).addClass('cell--complete');
          },
          orderable: false
        },
        {
          targets: 3, // Column 4
          render: function (data, type, row) {
            return `<a href="#">${data}</a>`
          }
        },
        {
          targets: 4, // Column 5
        }
      ],
      orderMulti: true,
      columns: [
        {
          title: 'Timestamp',
          data: 'Timestamp',
        },
        {
          title: 'Column 2',
          data: 'Column2',
        },
        {
          title: 'Column 3',
          data: 'Column3',
        },
        {
          title: 'Column 4',
          data: 'Column4',
        },
        {
          title: 'Column 5',
          data: 'Column5',
        }
      ]
    }
  }
}
