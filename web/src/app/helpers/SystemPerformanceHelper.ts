import { ChartDatapoint, ChartDataModel } from "../shared/interfaces/chart.model"

// system errors
export const errorHistory: ChartDataModel[] = [
  {
    name: 'Total Errors',
    series: [
      {name: "Dec 10, 2023", value: 12},
      {name: "Dec 15, 2023", value: 17},
      {name: "Dec 20, 2023", value: 9},
      {name: "Dec 25, 2023", value: 6},
      {name: "Dec 30, 2023", value: 32},
      {name: "Jan 4, 2024", value: 27},
      {name: "Jan 9, 2024", value: 22},
    ]
  },
  {
    name: 'Validation Errors',
    series: [
      {name: "Dec 10, 2023", value: 7},
      {name: "Dec 15, 2023", value: 9},
      {name: "Dec 20, 2023", value: 3},
      {name: "Dec 25, 2023", value: 5},
      {name: "Dec 30, 2023", value: 11},
      {name: "Jan 4, 2024", value: 26},
      {name: "Jan 9, 2024", value: 18},
    ]
  },
  {
    name: 'System Errors',
    series: [
      {name: "Dec 10, 2023", value: 5},
      {name: "Dec 15, 2023", value: 8},
      {name: "Dec 20, 2023", value: 6},
      {name: "Dec 25, 2023", value: 1},
      {name: "Dec 30, 2023", value: 21},
      {name: "Jan 4, 2024", value: 1},
      {name: "Jan 9, 2024", value: 4},
    ]
  }
]

// Report Generation
export const reportGenerationCurrent: ChartDatapoint[] = [
  {
    name: "Average Time per day",
    value: 10686
  }
]

export const reportGenerationHistory: ChartDataModel[] = [{
  name: 'Avg. Generation Time',
  series: [
    {name: "Dec 10, 2023", value: 754},
    {name: "Dec 15, 2023", value: 6327},
    {name: "Dec 20, 2023", value: 201},
    {name: "Dec 25, 2023", value: 9015},
    {name: "Dec 30, 2023", value: 3369},
    {name: "Jan 5, 2024", value: 4248},
    {name: "Jan 9, 2024", value: 10686}
  ]
}]

// Active Tenants

export const activeTenantCurrent: ChartDatapoint[] = [{
  name: "Total Tenants",
  value: 20
}]

export const activeTenantHistory: ChartDataModel[] = [{
  name: 'Active Tenants',
  series: [
    {name: "Dec 10, 2023", value: 5},
    {name: "Dec 15, 2023", value: 7},
    {name: "Dec 20, 2023", value: 11},
    {name: "Dec 25, 2023", value: 10},
    {name: "Dec 30, 2023", value: 10},
    {name: "Jan 5, 2024", value: 16},
    {name: "Jan 9, 2024", value: 20}
  ]
}]