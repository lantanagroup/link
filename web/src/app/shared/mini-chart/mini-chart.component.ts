import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgxChartsModule } from '@swimlane/ngx-charts';

@Component({
  selector: 'app-mini-chart',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './mini-chart.component.html',
  styleUrls: ['./mini-chart.component.css']
})
export class MiniChartComponent {
  @Input() data: any[] = [];

  // options
  legend: boolean = false;
  showLabels: boolean = false;
  animations: boolean = true;
  xAxis: boolean = false;
  yAxis: boolean = false;
  showYAxisLabel: boolean = false;
  showXAxisLabel: boolean = false;
  xAxisLabel: string = 'X';
  yAxisLabel: string = 'Y';
  timeline: boolean = false;

  colorScheme = {
    domain: ['#497d0c']
  }

  
}
