import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-section-heading',
  inputs: ['level', 'heading', 'headingClasses', 'class'],
  standalone: true,
  imports: [CommonModule],
  templateUrl: './section-heading.component.html',
  styleUrls: ['./section-heading.component.scss']
})
export class SectionHeadingComponent {
  @Input() level: 1 | 2 | 3 | 4 | 5 | 6 = 2;
  @Input() heading!: string;
  @Input() headingClasses?: string = '';
  @Input() class?: string = '';
}
