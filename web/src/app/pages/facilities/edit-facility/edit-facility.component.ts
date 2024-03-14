import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { HeroComponent } from 'src/app/shared/hero/hero.component';
import { SectionComponent } from 'src/app/shared/section/section.component';
import { FormUpdateFacilityComponent } from 'src/app/shared/form-update-facility/form-update-facility.component';

@Component({
  selector: 'app-edit-facility',
  standalone: true,
  imports: [CommonModule, HeroComponent, SectionComponent, FormUpdateFacilityComponent ],
  templateUrl: './edit-facility.component.html',
  styleUrls: ['./edit-facility.component.scss']
})
export class EditFacilityComponent {
  facilityId: string | null = null


  constructor(private route: ActivatedRoute) {}

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      this.facilityId = params.get('id')
    })
  }

  generateCancelLink(): { url: string } {
    const url = this.facilityId
      ? `/facilities/facility/${this.facilityId}`
      : '/facilities';

    return { url: url };
  }
}
