import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HeroComponent } from 'src/app/shared/hero/hero.component';
import { SectionComponent } from 'src/app/shared/section/section.component';
import { SectionHeadingComponent } from 'src/app/shared/section-heading/section-heading.component';
import { ButtonComponent } from 'src/app/shared/button/button.component';
import { IconComponent } from 'src/app/shared/icon/icon.component';

@Component({
  selector: 'app-add-facility',
  standalone: true,
  imports: [CommonModule, HeroComponent, SectionComponent, SectionHeadingComponent, ButtonComponent, IconComponent],
  templateUrl: './add-facility.component.html',
  styleUrls: ['./add-facility.component.scss']
})
export class AddFacilityComponent {

}
