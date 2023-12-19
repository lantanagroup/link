import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { HeroComponent } from 'src/app/shared/hero/hero.component';
import { SectionComponent } from 'src/app/shared/section/section.component';
import { SectionHeadingComponent } from 'src/app/shared/section-heading/section-heading.component';
import { ButtonComponent } from 'src/app/shared/button/button.component';
import { IconComponent } from 'src/app/shared/icon/icon.component';

@Component({
  selector: 'app-facility',
  standalone: true,
  imports: [CommonModule, HeroComponent, SectionComponent, SectionHeadingComponent, ButtonComponent, IconComponent],
  templateUrl: './facility.component.html',
  styleUrls: ['./facility.component.scss']
})
export class FacilityComponent {
  facilityId: string | null = null


  constructor(
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      this.facilityId = params.get('id')

      if(this.facilityId) {
        // todo : make API call
      } else {
        this.router.navigate(['/facilities'])
      }
    })
  }

  generateEditLink(): { url: string } {
    const url = this.facilityId
      ? `/facilities/edit-facility/${this.facilityId}`
      : '/facilities/add-facility';

    return { url: url };
  }
}
