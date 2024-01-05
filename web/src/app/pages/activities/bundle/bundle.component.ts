import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
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

@Component({
  selector: 'app-bundle',
  standalone: true,
  imports: [CommonModule, HeroComponent, ButtonComponent, SectionComponent, TabContainerComponent, TabComponent, CardComponent, MiniContentComponent, IconComponent, TableComponent, LinkComponent],
  templateUrl: './bundle.component.html',
  styleUrls: ['./bundle.component.scss']
})
export class BundleComponent {
  bundleId: string | null = '362574'
  tenantId: string | null = 'ehr-test'

  // purely placeholder
  bundleDetails: any = {
    submittedOn: '04.01.23 16:29:00 PM EST',
    reportingPeriod: '03.23.23-03.30.23',
    facility: {
      name: 'EHR Test On Prem',
      facilityId: this.tenantId,
      cdcOrgId: '1234'
    },
    normalizations: [
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
    ]
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router
  ) {}

  async ngOnInit() {
    this.route.paramMap.subscribe(params => {
      this.bundleId = params.get('bundleId')
      this.tenantId = params.get('tenantId')

      if(this.bundleId && this.tenantId) {
        // todo : make API call
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
}
