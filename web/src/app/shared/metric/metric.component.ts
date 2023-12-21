import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChartDatapoint, ChartDataModel } from '../interfaces/chart.model';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { curveBasis } from 'd3-shape';
import { IconComponent } from '../icon/icon.component';

@Component({
  selector: 'app-metric',
  standalone: true,
  imports: [CommonModule, NgxChartsModule, IconComponent],
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
  @Input() isGood?: boolean = true; // this is temporary

  getArrowIconPath(): string {
    let iconPath = '../../../assets/icons/';
    if(this.isGood) {
      iconPath += 'arrow-up-fat-green.svg';
    } else {
      iconPath += 'arrow-up-fat-red.svg';
    }
    return iconPath;
  }

  // chart render

  // options
  view: [number, number] = [100, 60];
  xAxisLabel: string = 'X';
  yAxisLabel: string = 'Y';
  curve: any = curveBasis;
  colorScheme: any = {
    name: 'success',
    selectable: false,
    domain: ['#497d0c']
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
