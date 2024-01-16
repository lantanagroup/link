import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { HeroComponent } from 'src/app/shared/hero/hero.component';
import { ButtonComponent } from 'src/app/shared/button/button.component';
import { IconComponent } from 'src/app/shared/icon/icon.component';
import { CardComponent } from 'src/app/shared/card/card.component';
import { ChartDatapoint, ChartDataModel } from 'src/app/shared/interfaces/chart.model';
import { SecondsToTimestamp } from 'src/app/helpers/GlobalPipes.pipe';
// placeholder data
import { errorHistory, reportGenerationCurrent, reportGenerationHistory, activeTenantCurrent, activeTenantHistory } from 'src/app/helpers/SystemPerformanceHelper';

@Component({
  selector: 'app-system-performance',
  standalone: true,
  imports: [CommonModule, HeroComponent, ButtonComponent, IconComponent, CardComponent, NgxChartsModule, SecondsToTimestamp],
  templateUrl: './system-performance.component.html',
  styleUrls: ['./system-performance.component.scss']
})
export class SystemPerformanceComponent {
  cardColors: any = {
    background: '#333',
    band: '#497d0c',
    text: '#f5f3f3'
  }
  lineChartColorScheme: any = {
    domain: ['#005eaa', '#497d0c', '#712177', '#29434e']
  };
  secondsToTimestamp = new SecondsToTimestamp

  // chart data
  errorHistory: ChartDataModel[] = errorHistory
  totalErrorRefs: ChartDatapoint[] = []

  reportGenerationCurrent: ChartDatapoint[] = reportGenerationCurrent
  reportGenerationHistory: ChartDataModel[] = reportGenerationHistory
  reportGenerationRefs: ChartDatapoint[] = []

  activeTenantCurrent: ChartDatapoint[] = activeTenantCurrent
  activeTenantHistory: ChartDataModel[] = activeTenantHistory
  activeTenantHistoryRefs: ChartDatapoint[] = []

  ngOnInit() {
    this.totalErrorRefs = this.getReferenceLines(this.errorHistory[0])

    this.reportGenerationRefs = this.getReferenceLines(this.reportGenerationHistory[0], this.secondsToTimestamp.transform)

    this.activeTenantHistoryRefs = this.getReferenceLines(this.activeTenantHistory[0])
  }

  getReferenceLines = (data: ChartDataModel, displayFunc: Function | void) => {
    let values = data.series.map(item => item.value)

    let valueMap = {
      min: Math.min(...values),
      max: Math.max(...values),
      average: values.reduce((acc, val) => acc + val, 0) / values.length
    }
    let displayMap = {
      min: valueMap.min,
      max: valueMap.max,
      average: parseFloat(valueMap.average.toFixed(2))
    }

    if(displayFunc) {
      displayMap = {
        min: displayFunc(valueMap.min),
        max: displayFunc(valueMap.max),
        average: displayFunc(parseFloat(valueMap.average.toFixed(0)))
      }
    }

    let refLines = [
      {
        name: `min: ${displayMap.min}`,
        value: valueMap.min
      },
      {
        name: `max: ${displayMap.max}`,
        value: valueMap.max
      },
      {
        name: `average: ${displayMap.average}`,
        value: valueMap.average
      }
    ]

    return refLines
  }
}
