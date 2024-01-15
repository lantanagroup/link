
export interface MetricData {
  average?: number,
  total?: number,
  history: number[]
}

export interface MetricCard {
  name: string,
  subText: string,
  changeWindow: string,
  upGood: boolean,
  toTimestamp?: boolean,
  metricData: MetricData
}

export enum TimePeriod {
  LastWeek = 'lastWeek',
  LastMonth = 'lastMonth',
  LastQuarter = 'lastQuarter',
  LastYear = 'lastYear'
}