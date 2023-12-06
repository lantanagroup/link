import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HeroComponent } from 'src/app/shared/hero/hero.component';
import { SectionHeadingComponent } from 'src/app/shared/section-heading/section-heading.component';
import { CardComponent } from 'src/app/shared/card/card.component';
import { MetricComponent } from 'src/app/shared/metric/metric.component';
import { ButtonComponent } from 'src/app/shared/button/button.component';
import { IconComponent } from 'src/app/shared/icon/icon.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, HeroComponent, ButtonComponent, IconComponent, CardComponent, MetricComponent, SectionHeadingComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent {

}
