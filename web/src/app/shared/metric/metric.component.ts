import { Component, Input, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MiniChartComponent } from 'src/app/shared/mini-chart/mini-chart.component';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faArrowUp, faArrowDown } from '@fortawesome/free-solid-svg-icons';
import { faUp, faDown } from '@fortawesome/pro-solid-svg-icons';

@Component({
  selector: 'app-metric',
  standalone: true,
  imports: [CommonModule, MiniChartComponent, FontAwesomeModule],
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

  iconUp = faUp ? faUp : faArrowUp
  iconDown = faDown ? faDown : faArrowDown
}
