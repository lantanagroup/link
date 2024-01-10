const averageQueryTime = {
  "name": "Average Query Time",
  "highlight": "6.1",
  "subText": "hours on average past 7 days",
  "changeValue": "18%",
  "direction": "up",
  "upGood": true,
  "series": [
    {"value": 4118, "name": "Data 1"},
    {"value": 5479, "name": "Data 2"},
    {"value": 6966, "name": "Data 3"},
    {"value": 9205, "name": "Data 4"},
    {"value": 6830, "name": "Data 5"},
    {"value": 8251, "name": "Data 6"},
    {"value": 9896, "name": "Data 7"}
  ]   
}
const preQualificationIssues = {
  "name": "Pre-Qualification Issues",
  "highlight": "99",
  "subText": "errors per 1,000 patients",
  "changeValue": "2%",
  "direction": "down",
  "upGood": false,
  "series": [
    {"value": 4118, "name": "Data 1"},
    {"value": 6370, "name": "Data 2"},
    {"value": 2568, "name": "Data 3"},
    {"value": 5326, "name": "Data 4"},
    {"value": 5017, "name": "Data 5"},
    {"value": 4916, "name": "Data 6"}
  ]  
}
const totalPatientsQueried = {
  "name": "Total Patients Queried",
  "highlight": "23k",
  "subText": "patients queried past 7 days",
  "changeValue": "33%",
  "direction": "up",
  "upGood": true,
  "series": [
    {"value": 4118, "name": "Data 1"},
    {"value": 5479, "name": "Data 2"},
    {"value": 7305, "name": "Data 3"},
    {"value": 9706, "name": "Data 4"},
    {"value": 6448, "name": "Data 5"},
    {"value": 8157, "name": "Data 6"}
  ]  
}
const averageValidationTime = {
  "name": "Average Validation Time",
  "highlight": "3.2",
  "subText": "hours on average past 7 days",
  "changeValue": "10%",
  "direction": "down",
  "upGood": false,
  "series": [
    {"value": 4118, "name": "Data 1"},
    {"value": 5809, "name": "Data 2"},
    {"value": 4691, "name": "Data 3"},
    {"value": 6135, "name": "Data 4"},
    {"value": 3706, "name": "Data 5"},
    {"value": 2978, "name": "Data 6"},
    {"value": 2520, "name": "Data 7"},
    {"value": 3386, "name": "Data 8"}
  ]  
}

const totalSuccessfulSubmissions = {
  "name": "Total Successful Submissions",
  "highlight": "654",
  "subText": "submissions month to date",
  "changeValue": "20%",
  "direction": "up",
  "upGood": true,
  "series": [
    {"value": 1000, "name": "Data 1"},
    {"value": 1200, "name": "Data 2"},  // 20% increase
    {"value": 1152, "name": "Data 3"},  // 4% decrease
    {"value": 1290, "name": "Data 4"},  // 12% increase
    {"value": 1161, "name": "Data 5"},  // 10% decrease
    {"value": 1393, "name": "Data 6"},  // 20% increase
    {"value": 1337, "name": "Data 7"},  // 4% decrease
    {"value": 1497, "name": "Data 8"}   // 12% increase
  ]
}

const averageReportGeneration = {
  "name": "Average Report Generation",
  "highlight": "3.2",
  "subText": "hours on average month to date",
  "changeValue": "4%",
  "direction": "down",
  "upGood": false,
  "series": [
    {"value": 2000, "name": "Data 1"},
    {"value": 2400, "name": "Data 2"},  // 20% increase
    {"value": 2304, "name": "Data 3"},  // 4% decrease
    {"value": 2578, "name": "Data 4"},  // 12% increase
    {"value": 2320, "name": "Data 5"}   // 10% decrease
  ]
}

const averageQueryTime2 = {
  "name": "Average Query Time",
  "highlight": "2.4",
  "subText": "hours on average past 7 days",
  "changeValue": "12%",
  "direction": "up",
  "upGood": false,
  "series": [
    {"value": 1500, "name": "Data 1"},
    {"value": 1800, "name": "Data 2"},  // 20% increase
    {"value": 1728, "name": "Data 3"},  // 4% decrease
    {"value": 1935, "name": "Data 4"},  // 12% increase
    {"value": 1742, "name": "Data 5"},  // 10% decrease
    {"value": 2090, "name": "Data 6"},  // 20% increase
    {"value": 2006, "name": "Data 7"},  // 4% decrease
    {"value": 2247, "name": "Data 8"},  // 12% increase
    {"value": 2022, "name": "Data 9"},  // 10% decrease
    {"value": 2426, "name": "Data 10"}  // 20% increase
  ]
}

const averageValidationTime2 = {
  "name": "Average Validation Time",
  "highlight": "3.2",
  "subText": "hours on average past 7 days",
  "changeValue": "10%",
  "direction": "down",
  "upGood": false,
  "series": [
    {"value": 2500, "name": "Data 1"},
    {"value": 3000, "name": "Data 2"},  // 20% increase
    {"value": 2880, "name": "Data 3"},  // 4% decrease
    {"value": 3226, "name": "Data 4"},  // 12% increase
    {"value": 2903, "name": "Data 5"},  // 10% decrease
    {"value": 3484, "name": "Data 6"},  // 20% increase
    {"value": 3345, "name": "Data 7"}   // 4% decrease
  ]
}

export const recentActivityData = [
  averageQueryTime,
  preQualificationIssues,
  totalPatientsQueried,
  averageValidationTime
]

export const activitiesData = [
  totalSuccessfulSubmissions,
  averageReportGeneration,
  averageQueryTime2,
  averageValidationTime2
]