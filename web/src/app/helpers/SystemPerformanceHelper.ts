import { ChartDatapoint, ChartDataModel } from "../shared/interfaces/chart.model"

// system errors
export const errorHistory: ChartDataModel[] = [
  {
    name: 'Total Errors',
    series: [
      {name: "Dec 11, 2023", value: 23},
      {name: "Dec 12, 2023", value: 17},
      {name: "Dec 13, 2023", value: 20},
      {name: "Dec 14, 2023", value: 25},
      {name: "Dec 15, 2023", value: 19},
      {name: "Dec 16, 2023", value: 21},
      {name: "Dec 17, 2023", value: 27},
      {name: "Dec 18, 2023", value: 22},
      {name: "Dec 19, 2023", value: 24},
      {name: "Dec 20, 2023", value: 30},
      {name: "Dec 21, 2023", value: 18},
      {name: "Dec 22, 2023", value: 26},
      {name: "Dec 23, 2023", value: 21},
      {name: "Dec 24, 2023", value: 29},
      {name: "Dec 25, 2023", value: 23},
      {name: "Dec 26, 2023", value: 20},
      {name: "Dec 27, 2023", value: 31},
      {name: "Dec 28, 2023", value: 28},
      {name: "Dec 29, 2023", value: 22},
      {name: "Dec 30, 2023", value: 32},
      {name: "Dec 31, 2023", value: 30},
      {name: "Jan 1, 2024", value: 24},
      {name: "Jan 2, 2024", value: 27},
      {name: "Jan 3, 2024", value: 25},
      {name: "Jan 4, 2024", value: 33},
      {name: "Jan 5, 2024", value: 26},
      {name: "Jan 6, 2024", value: 34},
      {name: "Jan 7, 2024", value: 29},
      {name: "Jan 8, 2024", value: 35},
      {name: "Jan 9, 2024", value: 37},
    ]
  },  
  {
    name: 'Validation Errors',
    series: [
      {name: "Dec 11, 2023", value: 18},
      {name: "Dec 12, 2023", value: 10},
      {name: "Dec 13, 2023", value: 12},
      {name: "Dec 14, 2023", value: 15},
      {name: "Dec 15, 2023", value: 12},
      {name: "Dec 16, 2023", value: 16},
      {name: "Dec 17, 2023", value: 20},
      {name: "Dec 18, 2023", value: 13},
      {name: "Dec 19, 2023", value: 15},
      {name: "Dec 20, 2023", value: 22},
      {name: "Dec 21, 2023", value: 11},
      {name: "Dec 22, 2023", value: 20},
      {name: "Dec 23, 2023", value: 16},
      {name: "Dec 24, 2023", value: 22},
      {name: "Dec 25, 2023", value: 18},
      {name: "Dec 26, 2023", value: 13},
      {name: "Dec 27, 2023", value: 24},
      {name: "Dec 28, 2023", value: 22},
      {name: "Dec 29, 2023", value: 13},
      {name: "Dec 30, 2023", value: 25},
      {name: "Dec 31, 2023", value: 23},
      {name: "Jan 1, 2024", value: 15},
      {name: "Jan 2, 2024", value: 18},
      {name: "Jan 3, 2024", value: 16},
      {name: "Jan 4, 2024", value: 26},
      {name: "Jan 5, 2024", value: 17},
      {name: "Jan 6, 2024", value: 27},
      {name: "Jan 7, 2024", value: 22},
      {name: "Jan 8, 2024", value: 28},
      {name: "Jan 9, 2024", value: 30},
    ]
  },  
  {
    name: 'System Errors',
    series: [
      {name: "Dec 11, 2023", value: 5},
      {name: "Dec 12, 2023", value: 7},
      {name: "Dec 13, 2023", value: 8},
      {name: "Dec 14, 2023", value: 10},
      {name: "Dec 15, 2023", value: 7},
      {name: "Dec 16, 2023", value: 5},
      {name: "Dec 17, 2023", value: 7},
      {name: "Dec 18, 2023", value: 9},
      {name: "Dec 19, 2023", value: 9},
      {name: "Dec 20, 2023", value: 8},
      {name: "Dec 21, 2023", value: 7},
      {name: "Dec 22, 2023", value: 6},
      {name: "Dec 23, 2023", value: 5},
      {name: "Dec 24, 2023", value: 7},
      {name: "Dec 25, 2023", value: 5},
      {name: "Dec 26, 2023", value: 7},
      {name: "Dec 27, 2023", value: 7},
      {name: "Dec 28, 2023", value: 6},
      {name: "Dec 29, 2023", value: 9},
      {name: "Dec 30, 2023", value: 7},
      {name: "Dec 31, 2023", value: 7},
      {name: "Jan 1, 2024", value: 9},
      {name: "Jan 2, 2024", value: 9},
      {name: "Jan 3, 2024", value: 9},
      {name: "Jan 4, 2024", value: 7},
      {name: "Jan 5, 2024", value: 9},
      {name: "Jan 6, 2024", value: 7},
      {name: "Jan 7, 2024", value: 7},
      {name: "Jan 8, 2024", value: 7},
      {name: "Jan 9, 2024", value: 7},
    ]
  }  
]

// Report Generation
export const reportGenerationCurrent: ChartDatapoint[] = [
  {
    name: "Average Time per day",
    value: 10432
  }
]

export const reportGenerationHistory: ChartDataModel[] = [{
  name: 'Avg. Generation Time',
  series: [
    {name: "Dec 11, 2023", value: 2895},
    {name: "Dec 12, 2023", value: 7324},
    {name: "Dec 13, 2023", value: 11478},
    {name: "Dec 14, 2023", value: 5820},
    {name: "Dec 15, 2023", value: 3639},
    {name: "Dec 16, 2023", value: 12095},
    {name: "Dec 17, 2023", value: 2736},
    {name: "Dec 18, 2023", value: 9103},
    {name: "Dec 19, 2023", value: 8560},
    {name: "Dec 20, 2023", value: 10588},
    {name: "Dec 21, 2023", value: 12456},
    {name: "Dec 22, 2023", value: 7320},
    {name: "Dec 23, 2023", value: 6075},
    {name: "Dec 24, 2023", value: 8691},
    {name: "Dec 25, 2023", value: 13123},
    {name: "Dec 26, 2023", value: 4980},
    {name: "Dec 27, 2023", value: 8764},
    {name: "Dec 28, 2023", value: 3235},
    {name: "Dec 29, 2023", value: 4798},
    {name: "Dec 30, 2023", value: 9981},
    {name: "Dec 31, 2023", value: 13240},
    {name: "Jan 1, 2024", value: 6731},
    {name: "Jan 2, 2024", value: 7453},
    {name: "Jan 3, 2024", value: 9835},
    {name: "Jan 4, 2024", value: 11209},
    {name: "Jan 5, 2024", value: 14399},
    {name: "Jan 6, 2024", value: 6543},
    {name: "Jan 7, 2024", value: 8074},
    {name: "Jan 8, 2024", value: 12258},
    {name: "Jan 9, 2024", value: 7560},
    {name: "Jan 10, 2024", value: 10432}
  ]  
}]

// Active Tenants

export const activeTenantCurrent: ChartDatapoint[] = [{
  name: "Total Tenants",
  value: 21
}]

export const activeTenantHistory: ChartDataModel[] = [{
  name: 'Active Tenants',
  series: [
    {name: "Dec 11, 2023", value: 8},
    {name: "Dec 12, 2023", value: 9},
    {name: "Dec 13, 2023", value: 6},
    {name: "Dec 14, 2023", value: 7},
    {name: "Dec 15, 2023", value: 7},
    {name: "Dec 16, 2023", value: 8},
    {name: "Dec 17, 2023", value: 9},
    {name: "Dec 18, 2023", value: 10},
    {name: "Dec 19, 2023", value: 5},
    {name: "Dec 20, 2023", value: 11},
    {name: "Dec 21, 2023", value: 12},
    {name: "Dec 22, 2023", value: 9},
    {name: "Dec 23, 2023", value: 8},
    {name: "Dec 24, 2023", value: 7},
    {name: "Dec 25, 2023", value: 10},
    {name: "Dec 26, 2023", value: 11},
    {name: "Dec 27, 2023", value: 12},
    {name: "Dec 28, 2023", value: 13},
    {name: "Dec 29, 2023", value: 14},
    {name: "Dec 30, 2023", value: 10},
    {name: "Dec 31, 2023", value: 15},
    {name: "Jan 1, 2024", value: 14},
    {name: "Jan 2, 2024", value: 13},
    {name: "Jan 3, 2024", value: 12},
    {name: "Jan 4, 2024", value: 11},
    {name: "Jan 5, 2024", value: 16},
    {name: "Jan 6, 2024", value: 17},
    {name: "Jan 7, 2024", value: 18},
    {name: "Jan 8, 2024", value: 19},
    {name: "Jan 9, 2024", value: 20},
    {name: "Jan 10, 2024", value: 21}
  ]  
}]