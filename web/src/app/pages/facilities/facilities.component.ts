import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HeroComponent } from 'src/app/shared/hero/hero.component';
import { AccordionComponent } from 'src/app/shared/accordion/accordion.component';
import { IconComponent } from 'src/app/shared/icon/icon.component';
import { ButtonComponent } from 'src/app/shared/button/button.component';

@Component({
  selector: 'app-facilities',
  standalone: true,
  imports: [CommonModule, HeroComponent, IconComponent, ButtonComponent, AccordionComponent],
  templateUrl: './facilities.component.html',
  styleUrls: ['./facilities.component.css']
})
export class FacilitiesComponent {

}
