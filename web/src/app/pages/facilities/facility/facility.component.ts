import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { HeroComponent } from 'src/app/shared/hero/hero.component';
import { SectionComponent } from 'src/app/shared/section/section.component';
import { SectionHeadingComponent } from 'src/app/shared/section-heading/section-heading.component';
import { ButtonComponent } from 'src/app/shared/button/button.component';
import { IconComponent } from 'src/app/shared/icon/icon.component';
import { FacilitiesApiService } from 'src/services/api/facilities/facilities-api.service';
import { CardComponent } from "../../../shared/card/card.component";

@Component({
    selector: 'app-facility',
    standalone: true,
    templateUrl: './facility.component.html',
    styleUrls: ['./facility.component.scss'],
    imports: [CommonModule, HeroComponent, SectionComponent, SectionHeadingComponent, ButtonComponent, IconComponent, CardComponent]
})
export class FacilityComponent {
  facilityId: string | null = null
  facilityDetails: any = null;


  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private facilitiesApiService: FacilitiesApiService
  ) {}

  async ngOnInit() {
    this.route.paramMap.subscribe(params => {
      this.facilityId = params.get('id')

      if(this.facilityId) {
        // todo : make API call
        this.GetFacilityDetails(this.facilityId);
      } else {
        this.router.navigate(['/facilities'])
      }
    })
  }

  async GetFacilityDetails(id: string) {
    try {
      const tenantDetail = await this.facilitiesApiService.fetchFacilityById(id);
      this.facilityDetails = tenantDetail;
    } catch (error) {
      console.error('Error Loading table data.', error);
    }
  }

  generateEditLink(): { url: string } {
    const url = this.facilityId
      ? `/facilities/edit-facility/${this.facilityId}`
      : '/facilities/add-facility';

    return { url: url };
  }
}
