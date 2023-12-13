import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faArrowUp, faArrowDown, faThList } from '@fortawesome/free-solid-svg-icons';
import { faUp, faDown } from '@fortawesome/pro-solid-svg-icons';
import { ChartDatapoint, ChartDataModel } from '../interfaces/chart.model';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { ThisReceiver } from '@angular/compiler';

@Component({
  selector: 'app-metric',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule, NgxChartsModule],
  templateUrl: './metric.component.html',
  styleUrls: ['./metric.component.scss']
})
export class MetricComponent {
  @Input() mainValue: string = ''; // The biggest number that we see on the design
  @Input() subText: string = ''; // Context of what the main value is compared to
  @Input() changeValue: string = ''; // The rate of value changed
  @Input() changeWindow?: string = 'month'; // the duration of value changed
  @Input() isValueUp: boolean = true; // This will change the path of image
  @Input() data: ChartDatapoint[] = [];

  iconUp = faUp ? faUp : faArrowUp
  iconDown = faDown ? faDown : faArrowDown

  // chart render

  // options
  view: [number, number] = [100, 60];
  xAxisLabel: string = 'X';
  yAxisLabel: string = 'Y';
  colorScheme: {} = {
    domain: ['#497d0c', '#497d0c']
  };

  miniChartData: ChartDataModel[] = [];

  ngOnInit() {
    this.miniChartData = this.setUpChartData()
  }

  setUpChartData = () => {
    const chartData = [{
      name: 'chart data',
      series: this.data
    }]

    return chartData
  }
}
