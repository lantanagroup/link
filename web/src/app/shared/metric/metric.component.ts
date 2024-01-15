import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChartDatapoint, ChartDataModel } from '../interfaces/chart.model';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { curveBasis } from 'd3-shape';
import { IconComponent } from '../icon/icon.component';
import { MetricData } from '../interfaces/metrics.model';
import { SecondsToHours, RoundToThousand } from 'src/app/helpers/GlobalPipes.pipe';

interface rateOfChange {
  value: string
  isUp: boolean
}

@Component({
  selector: 'app-metric',
  standalone: true,
  imports: [CommonModule, NgxChartsModule, IconComponent],
  templateUrl: './metric.component.html',
  styleUrls: ['./metric.component.scss']
})
export class MetricComponent {
  @Input() subText: string = ''; // Context of what the main value is compared to
  @Input() changeWindow?: string = 'month'; // the duration of value changed
  @Input() isUpGood: boolean = true;
  @Input() metricData!: MetricData;
  @Input() toTimestamp?: boolean = false; // converts seconds to timestamp
  change: rateOfChange | undefined = undefined
  currentValue: number | string | undefined = undefined;
  
  private secondsToHours = new SecondsToHours
  private roundToThousand = new RoundToThousand

  @Input() data: ChartDatapoint[] = []; // will come from api

  getArrowIconPath(): string {
    let iconPath = '../../../assets/icons/';
    if(this.isUpGood === this.change?.isUp) {
      iconPath += 'arrow-up-fat-green.svg';
    } else {
      iconPath += 'arrow-up-fat-red.svg';
    }
    return iconPath;
  }

  // chart render
  graphColor = this.isUpGood === this.change?.isUp ? '#497d0c' : '#af4448';

  // options
  view: [number, number] = [100, 60];
  xAxisLabel: string = 'X';
  yAxisLabel: string = 'Y';
  curve: any = curveBasis;
  colorScheme: any = {
    name: 'success',
    selectable: false,
    domain: [this.graphColor]
  };

  miniChartData: ChartDataModel[] = [];

  ngOnInit() {
    this.currentValue = this.metricData?.average ? this.metricData.average : this.metricData?.total
    
    if(this.currentValue) {
      this.currentValue = parseFloat(Math.abs(this.currentValue).toFixed(0))
      this.change = this.calculateChange(this.currentValue)

      this.colorScheme.domain = this.isUpGood === this.change?.isUp ? ['#497d0c'] : ['#af4448'];
      this.miniChartData = this.setUpChartData(this.currentValue)
      
      if(this.toTimestamp) {
        this.currentValue = this.secondsToHours.transform(this.currentValue)
      } else {
        this.currentValue = this.roundToThousand.transform(this.currentValue)
      }
    }

  }

  calculateChange(currentValue: number): rateOfChange {
    const change = ((currentValue - this.metricData.history[0]) / this.metricData.history[0]) * 100,
          isUp = change > 0 
    
    return ({
      value: parseFloat(Math.abs(change).toFixed(0)).toString() + '%',
      isUp: isUp
    })
  }

  setUpChartData = (currentValue: number) => {
    const revHistory = this.metricData.history.reverse(),
          data = revHistory.map((value: number, i: number) => ({value: value, name: `index ${i}`}))

    data.push({name: 'today', value: currentValue})

    const chartData = [{
      name: 'chart data',
      series: data
    }]

    return chartData
  }
}
