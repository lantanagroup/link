import { Report } from "../shared/interfaces/report.model";


type StatusType = 'pending' | 'completed' | 'failed';

// Below funcitons are all helper functions. All of these function could be moved to a separate helper file or something.
export function generateRandomData(numEntries: number): Report[] {
  const facilities = ["University of Minnesota", "University of South Dakota", "University of Wisconsin"];
  const measuresArray = ["ABC", "DE", "ABE", "REW"];
  const statusOptions: StatusType[] = ["pending", "completed", "failed"];
  const activitiesMap: { [K in StatusType]: string } = {
    "pending": "Submission Initiated",
    "completed": "Successful Submission",
    "failed": "Failed Submission"
  };

  const randomData: Report[] = [];

  for (let i = 0; i < numEntries; i++) {
    const status: StatusType = statusOptions[Math.floor(Math.random() * statusOptions.length)];
    const activity = activitiesMap[status];
    const measures = status === 'completed' ? getRandomSubarray(measuresArray, Math.ceil(Math.random() * measuresArray.length)) : (status === 'pending' ? ['Pending'] : ['n/a']);
    const aggregates = status === 'completed' ? generateRandomNumbersAsStrings(5) : ['--'];
    const { periodStart, periodEnd } = generateRandomPeriod()

    randomData.push({
      id: createGUID(),
      reportId: createGUID(),
      submittedTime: generateRandomDateTime(),
      generatedTime: generateRandomDateTime(),
      activity: activity,
      details: (Math.floor(Math.random() * (9999999 - 1000000 + 1)) + 1000000).toString(),
      tenantName: facilities[Math.floor(Math.random() * facilities.length)],
      nhsnOrgId: (Math.floor(Math.random() * (9999999 - 1000000 + 1)) + 1000000).toString(),
      periodStart: periodStart,
      periodEnd: periodEnd,
      measureId: measures,
      aggregates: aggregates,
      status: status,
      version: "1.0",
      patientLists: [],
    });
  }
  return randomData;
}

export function getRandomSubarray(arr: any[], size: number) {
  const shuffled = arr.slice(0);
  let i = arr.length;
  const min = i - size;

  while (i-- > min) {
    const index = Math.floor((i + 1) * Math.random());
    const temp = shuffled[index];
    shuffled[index] = shuffled[i];
    shuffled[i] = temp;
  }

  return shuffled.slice(min);
}

export function generateRandomNumbersAsStrings(count: number): string[] {
  const numbers = [];
  for (let i = 0; i < count; i++) {
    // Generate a random number between 10,000 (inclusive) and 99,999 (inclusive)
    const randomNum = Math.floor(Math.random() * (9999 - 1000 + 1)) + 1000;
    numbers.push(randomNum.toLocaleString()); // Convert to string with commas
  }
  return numbers;
}

export function generateRandomPeriod() {
  const start = new Date(2023, Math.floor(Math.random() * 12), Math.floor(Math.random() * 28) + 1);
  const end = new Date(start);
  end.setDate(end.getDate() + 7);

  const formatDate = (date: any) => `${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}.${String(date.getFullYear()).substring(2)}`;
  return {
    periodStart: formatDate(start),
    periodEnd: formatDate(end)
  };
}

export function createGUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

export function generateRandomDateTime() {
  return `03.14.2023 ${Math.floor(Math.random() * 24)}:${Math.floor(Math.random() * 60)} ${Math.random() > 0.5 ? "AM" : "PM"} EST`;
}



export function calculatePeriodLength(periodStart: string, periodEnd: string): number {
  // Parse the dates from the format "MM.DD.YY" to JavaScript Date objects
  const startDate: Date = parseDate(periodStart);
  const endDate: Date = parseDate(periodEnd);

  // Calculate the difference in milliseconds and convert to days
  const diffInMilliseconds: number = endDate.getTime() - startDate.getTime();
  const diffInDays: number = diffInMilliseconds / (1000 * 60 * 60 * 24);

  return Math.round(diffInDays); // rounding to the nearest whole number
}

export function parseDate(dateStr: string): Date {
  const [month, day, year] = dateStr.split('.').map(Number);
  // Adjust year format from YY to YYYY (assuming 2000s)
  const fullYear: number = 2000 + year;
  return new Date(fullYear, month - 1, day); // Month is 0-indexed in JavaScript Date
}

export function getMeasuresBasedOnStatus(status: any, measuresOptions: any) {
  switch (status) {
    case 'pending':
      return ['Pending'];
    case 'failed':
      return ['n/a'];
    case 'completed':
      return getRandomSubarray(measuresOptions, Math.ceil(Math.random() * 3)); // Up to 3 measures
    default:
      return [];
  }
}

// Similarly for the other functions
