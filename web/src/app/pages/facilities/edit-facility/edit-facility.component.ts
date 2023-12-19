import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { HeroComponent } from 'src/app/shared/hero/hero.component';
import { SectionComponent } from 'src/app/shared/section/section.component';
import { SectionHeadingComponent } from 'src/app/shared/section-heading/section-heading.component';
import { ButtonComponent } from 'src/app/shared/button/button.component';
import { IconComponent } from 'src/app/shared/icon/icon.component';

@Component({
  selector: 'app-edit-facility',
  standalone: true,
  imports: [CommonModule, HeroComponent, SectionComponent, SectionHeadingComponent, ButtonComponent, IconComponent],
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
