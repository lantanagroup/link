export interface ChartDatapoint {
  value: number,
  name: string
}

export interface ChartDataModel {
  name: string,
  series: ChartDatapoint[]
}