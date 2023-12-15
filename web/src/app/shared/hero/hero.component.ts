import { Component, Input, TemplateRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SectionHeadingComponent } from '../section-heading/section-heading.component';
@Component({
  selector: 'app-hero',
  inputs: ['subHeading', 'pageTitle', 'color', 'cta', 'size'],
  standalone: true,
  imports: [CommonModule, SectionHeadingComponent],
  templateUrl: './hero.component.html',
  styleUrls: ['./hero.component.scss']
})
export class HeroComponent {

  @Input() subHeading?: string;
  @Input() pageTitle!: string;
  @Input() color?: 'blue' | 'green' | 'purple' | 'shade';
  @Input() cta?: TemplateRef<any>;
  @Input() size?: 'sm' | 'md' | 'lg' = 'sm';
}
