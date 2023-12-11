import { Component, Input, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MiniChartComponent } from 'src/app/shared/mini-chart/mini-chart.component';

@Component({
  selector: 'app-metric',
  standalone: true,
  imports: [CommonModule, MiniChartComponent],
  templateUrl: './metric.component.html',
  styleUrls: ['./metric.component.scss']
})
export class MetricComponent {
  @Input() mainValue: string = ''; // The biggest number that we see on the design
  @Input() subText: string = ''; // Context of what the main value is compared to
  @Input() changeValue: string = ''; // The rate of value changed
  @Input() changeWindow?: string = 'month'; // the duration of value changed
  @Input() isValueUp: boolean = true; // This will change the path of image
  @ViewChild(MiniChartComponent) chartComponent!: MiniChartComponent;

  get arrowIconPath(): string {
    // Determine the icon based on whether changeValue is positive or negative
    return this.isValueUp === true ? '/assets/icons/upArrow.svg' : '/assets/icons/downArrow.svg';
  }
}
