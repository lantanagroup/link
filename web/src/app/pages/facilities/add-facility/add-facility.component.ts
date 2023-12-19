import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HeroComponent } from 'src/app/shared/hero/hero.component';
import { SectionComponent } from 'src/app/shared/section/section.component';
import { FormUpdateFacilityComponent } from 'src/app/shared/form-update-facility/form-update-facility.component';

@Component({
  selector: 'app-add-facility',
  standalone: true,
  imports: [CommonModule, HeroComponent, SectionComponent, FormUpdateFacilityComponent],
  templateUrl: './add-facility.component.html',
  styleUrls: ['./add-facility.component.scss']
})
export class AddFacilityComponent {

}
